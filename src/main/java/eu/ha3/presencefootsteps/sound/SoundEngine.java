package eu.ha3.presencefootsteps.sound;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Either;
import eu.ha3.presencefootsteps.PFConfig;
import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.player.ImmediateSoundPlayer;
import eu.ha3.presencefootsteps.util.PlayerUtil;
import eu.ha3.presencefootsteps.world.Solver;
import eu.ha3.presencefootsteps.world.PFSolver;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;

public class SoundEngine implements IdentifiableResourceReloadListener {
    private static final Identifier ID = PresenceFootsteps.id("sounds");
    private static final Set<Identifier> BLOCKED_PLAYER_SOUNDS = Set.of(
            SoundEvents.ENTITY_PLAYER_SWIM.getId(),
            SoundEvents.ENTITY_PLAYER_SPLASH.getId(),
            SoundEvents.ENTITY_PLAYER_BIG_FALL.getId(),
            SoundEvents.ENTITY_PLAYER_SMALL_FALL.getId()
    );

    private Isolator isolator = new Isolator(this);
    private final Solver solver = new PFSolver(this);
    final ImmediateSoundPlayer soundPlayer = new ImmediateSoundPlayer(this);

    private final PFConfig config;

    private boolean hasConfigurations;

    public SoundEngine(PFConfig config) {
        this.config = config;
    }

    public float getVolumeForSource(LivingEntity source) {
        float volume = config.getGlobalVolume() / 100F;

        if (source instanceof PlayerEntity) {
            if (PlayerUtil.isClientPlayer(source)) {
                volume *= config.clientPlayerVolume.getPercentage();
            } else {
                volume *= config.otherPlayerVolume.getPercentage();
            }
        } else if (source instanceof HostileEntity) {
            volume *= config.hostileEntitiesVolume.getPercentage();
        } else {
            volume *= config.passiveEntitiesVolume.getPercentage();
        }

        float runningProgress = ((StepSoundSource) source).getStepGenerator(this)
                .map(generator -> generator.getMotionTracker().getSpeedScalingRatio(source))
                .orElse(0F);

        return volume * (1F + ((config.getRunningVolumeIncrease() / 100F) * runningProgress));
    }

    public Isolator getIsolator() {
        return isolator;
    }

    public Solver getSolver() {
        return solver;
    }

    public PFConfig getConfig() {
        return config;
    }

    public void reload() {
        if (config.getEnabled()) {
            reloadEverything(MinecraftClient.getInstance().getResourceManager());
        } else {
            shutdown();
        }
    }

    public boolean isEnabledFor(Entity entity) {
        return hasData() && isRunning(MinecraftClient.getInstance()) && config.getEntitySelector().test(entity);
    }

    public boolean hasData() {
        return hasConfigurations;
    }

    public boolean isRunning(MinecraftClient client) {
        return !client.isPaused() && isActive(client);
    }

    public boolean isActive(MinecraftClient client) {
        return hasData()
                && config.getEnabled()
                && (client.isInSingleplayer() || config.getEnabledMP());
    }

    private Stream<? extends Entity> getTargets(final Entity cameraEntity) {
        final List<? extends Entity> entities = cameraEntity.getWorld().getOtherEntities(null, cameraEntity.getBoundingBox().expand(16), e -> {
            return e instanceof LivingEntity
                    && !(e instanceof WaterCreatureEntity)
                    && !(e instanceof FlyingEntity)
                    && !(e instanceof ShulkerEntity
                            || e instanceof ArmorStandEntity
                            || e instanceof BoatEntity
                            || e instanceof AbstractMinecartEntity)
                        && !isolator.golems().contains(e.getType())
                        && !e.hasVehicle()
                        && !((LivingEntity)e).isSleeping()
                        && (!(e instanceof PlayerEntity) || !e.isSpectator())
                        && e.squaredDistanceTo(cameraEntity) <= 256
                        && config.getEntitySelector().test(e);
        });

        final Comparator<Entity> nearest = Comparator.comparingDouble(e -> e.squaredDistanceTo(cameraEntity));

        if (entities.size() < config.getMaxSteppingEntities()) {
            return entities.stream();
        }
        Set<Integer> alreadyVisited = new HashSet<>();
        return entities.stream()
            .sorted(nearest)
                    // Always play sounds for players and the entities closest to the camera
                        // If multiple entities share the same block, only play sounds for one of each distinct type
            .filter(e -> e == cameraEntity || e instanceof PlayerEntity || (alreadyVisited.size() < config.getMaxSteppingEntities() && alreadyVisited.add(Objects.hash(e.getType(), e.getBlockPos()))));
    }

    public void onFrame(MinecraftClient client, Entity cameraEntity) {
        if (isRunning(client)) {
            getTargets(cameraEntity).forEach(e -> {
                try {
                    ((StepSoundSource) e).getStepGenerator(this).ifPresent(generator -> {
                        generator.generateFootsteps();
                    });
                } catch (Throwable t) {
                    CrashReport report = CrashReport.create(t, "Generating PF sounds for entity");
                    CrashReportSection section = report.addElement("Entity being ticked");
                    if (e == null) {
                        section.add("Entity Type", "null");
                    } else {
                        e.populateCrashReport(section);
                        section.add("Entity's Locomotion Type", isolator.locomotions().lookup(e));
                        section.add("Entity is Golem", isolator.golems().contains(e.getType()));
                    }
                    config.populateCrashReport(report.addElement("PF Configuration"));
                    throw new CrashException(report);
                }
            });

            isolator.acoustics().think(); // Delayed sounds
        }
    }

    public boolean onSoundRecieved(PlaySoundS2CPacket packet) {
        @Nullable RegistryEntry<SoundEvent> event = packet.getSound();
        @Nullable ClientWorld world = MinecraftClient.getInstance().world;

        if (world == null || event == null || !isActive(MinecraftClient.getInstance())) {
            return false;
        }

        var stepAtPos = world.getBlockState(BlockPos.ofFloored(packet.getX(), packet.getY() - 1, packet.getZ())).getSoundGroup().getStepSound();
        var sound = Either.unwrap(event.getKeyOrValue().mapBoth(i -> i.getValue(), i -> i.getId()));

        return BLOCKED_PLAYER_SOUNDS.contains(sound)
                || (packet.getCategory() == SoundCategory.PLAYERS && sound.equals(stepAtPos.getId()));
    }

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer sync, ResourceManager sender,
                                          Profiler serverProfiler, Profiler clientProfiler,
                                          Executor serverExecutor, Executor clientExecutor) {
        return sync.whenPrepared(null).thenRunAsync(() -> {
            clientProfiler.startTick();
            clientProfiler.push("Reloading PF Sounds");
            reloadEverything(sender);
            clientProfiler.pop();
            clientProfiler.endTick();
        }, clientExecutor);
    }

    public void reloadEverything(ResourceManager manager) {
        shutdown();
        hasConfigurations = isolator.load(manager);
    }

    public void shutdown() {
        isolator = new Isolator(this);
        hasConfigurations = false;
    }
}
