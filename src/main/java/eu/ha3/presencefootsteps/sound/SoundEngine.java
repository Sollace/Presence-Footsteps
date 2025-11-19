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
import com.mojang.datafixers.util.Unit;

import eu.ha3.presencefootsteps.PFConfig;
import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.player.ImmediateSoundPlayer;
import eu.ha3.presencefootsteps.util.PlayerUtil;
import eu.ha3.presencefootsteps.world.Solver;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import eu.ha3.presencefootsteps.world.PFSolver;

public class SoundEngine implements PreparableReloadListener {
    public static final Identifier ID = PresenceFootsteps.id("sounds");
    private static final Set<Identifier> BLOCKED_PLAYER_SOUNDS = Set.of(
            SoundEvents.PLAYER_SWIM.location(),
            SoundEvents.PLAYER_SPLASH.location(),
            SoundEvents.PLAYER_BIG_FALL.location(),
            SoundEvents.PLAYER_SMALL_FALL.location()
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

        if (source instanceof Player) {
            if (PlayerUtil.isClientPlayer(source)) {
                volume *= config.clientPlayerVolume.getPercentage();
            } else {
                volume *= config.otherPlayerVolume.getPercentage();
            }
        } else if (source instanceof Monster) {
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
            reloadEverything(Minecraft.getInstance().getResourceManager());
        } else {
            shutdown();
        }
    }

    public boolean isEnabledFor(Entity entity) {
        return hasData() && isRunning(Minecraft.getInstance()) && config.getEntitySelector().test(entity);
    }

    public boolean hasData() {
        return hasConfigurations;
    }

    public boolean isRunning(Minecraft client) {
        return !client.isPaused() && isActive(client);
    }

    public boolean isActive(Minecraft client) {
        return hasData()
                && config.getEnabled()
                && (client.isSingleplayer() || config.getEnabledMP());
    }

    private Stream<? extends Entity> getTargets(final Entity cameraEntity) {
        final List<? extends Entity> entities = cameraEntity.level().getEntities((Entity)null, cameraEntity.getBoundingBox().inflate(16), e -> {
            return e instanceof LivingEntity
                    && !config.isIgnoredForFootsteps(e.getType())
                    && !(e instanceof AgeableWaterCreature)
                    && !(e instanceof Shulker
                            || e instanceof ArmorStand
                            || e instanceof Boat
                            || e instanceof AbstractMinecart)
                        && !isolator.golems().contains(e.getType())
                        && !e.isPassenger()
                        && !((LivingEntity)e).isSleeping()
                        && (!(e instanceof Player) || !e.isSpectator())
                        && e.distanceToSqr(cameraEntity) <= 256
                        && config.getEntitySelector().test(e);
        });

        final Comparator<Entity> nearest = Comparator.comparingDouble(e -> e.distanceToSqr(cameraEntity));

        if (entities.size() < config.getMaxSteppingEntities()) {
            return entities.stream();
        }
        Set<Integer> alreadyVisited = new HashSet<>();
        return entities.stream()
            .sorted(nearest)
                    // Always play sounds for players and the entities closest to the camera
                        // If multiple entities share the same block, only play sounds for one of each distinct type
            .filter(e -> e == cameraEntity || e instanceof Player || (alreadyVisited.size() < config.getMaxSteppingEntities() && alreadyVisited.add(Objects.hash(e.getType(), e.blockPosition()))));
    }

    public void onFrame(Minecraft client, Entity cameraEntity) {
        if (isRunning(client)) {
            getTargets(cameraEntity).forEach(e -> {
                try {
                    ((StepSoundSource) e).getStepGenerator(this).ifPresent(generator -> {
                        generator.generateFootsteps();
                    });
                } catch (Throwable t) {
                    CrashReport report = CrashReport.forThrowable(t, "Generating PF sounds for entity");
                    CrashReportCategory section = report.addCategory("Entity being ticked");
                    if (e == null) {
                        section.setDetail("Entity Type", "null");
                    } else {
                        e.fillCrashReportCategory(section);
                        section.setDetail("Entity's Locomotion Type", isolator.locomotions().lookup(e));
                        section.setDetail("Entity is Golem", isolator.golems().contains(e.getType()));
                    }
                    config.populateCrashReport(report.addCategory("PF Configuration"));
                    throw new ReportedException(report);
                }
            });

            isolator.acoustics().think(); // Delayed sounds
        }
    }

    public boolean onSoundRecieved(ClientboundSoundPacket packet) {
        @Nullable Holder<SoundEvent> event = packet.getSound();
        @Nullable ClientLevel world = Minecraft.getInstance().level;

        if (world == null || event == null || !isActive(Minecraft.getInstance())) {
            return false;
        }

        var stepAtPos = world.getBlockState(BlockPos.containing(packet.getX(), packet.getY() - 1, packet.getZ())).getSoundType().getStepSound();
        var sound = Either.unwrap(event.unwrap().mapBoth(i -> i.identifier(), i -> i.location()));

        return BLOCKED_PLAYER_SOUNDS.contains(sound)
                || (packet.getSource() == SoundSource.PLAYERS && sound.equals(stepAtPos.location()));
    }

    @Override
    public CompletableFuture<Void> reload(SharedState store, Executor prepareExecutor, PreparableReloadListener.PreparationBarrier fence, Executor applyExecutor) {
        return fence.wait(Unit.INSTANCE).thenRunAsync(() -> {
            ProfilerFiller profiler = Profiler.get();
            profiler.push("Reloading PF Sounds");
            reloadEverything(store.resourceManager());
            profiler.pop();
        }, applyExecutor);
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