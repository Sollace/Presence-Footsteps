package eu.ha3.presencefootsteps.sound.generator;

import org.jetbrains.annotations.Nullable;
import com.google.common.base.MoreObjects;

import eu.ha3.presencefootsteps.config.Variator;
import eu.ha3.presencefootsteps.mixins.ILivingEntity;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.util.Lerp;
import eu.ha3.presencefootsteps.util.PlayerUtil;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import eu.ha3.presencefootsteps.world.Association;
import eu.ha3.presencefootsteps.world.AssociationPool;
import eu.ha3.presencefootsteps.world.BiomeVarianceLookup;
import eu.ha3.presencefootsteps.world.Solver;
import eu.ha3.presencefootsteps.world.SoundsKey;
import eu.ha3.presencefootsteps.world.Substrates;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.equipment.Equippable;

class TerrestrialStepSoundGenerator implements StepSoundGenerator {
    // Footsteps
    protected float dmwBase;
    protected float dwmYChange;
    protected double yPosition;

    // Airborne
    protected boolean isAirborne;

    protected double lastFallDistance;
    protected float distanceReference;
    protected float lastReference;
    protected boolean isImmobile;
    protected long timeImmobile;

    protected long immobilePlayback;
    protected int immobileInterval;

    protected boolean isRightFoot;

    protected double xMovec;
    protected double zMovec;
    protected boolean scalStat;

    private boolean stepThisFrame;

    private boolean isMessyFoliage;
    private long brushesTime;

    protected final LivingEntity entity;
    protected final SoundEngine engine;
    private final Modifier<TerrestrialStepSoundGenerator> modifier;
    protected final MotionTracker motionTracker = new MotionTracker(this);
    protected final AssociationPool associations;

    private final Lerp biomePitch = new Lerp();
    private final Lerp biomeVolume = new Lerp();

    public TerrestrialStepSoundGenerator(LivingEntity entity, SoundEngine engine, Modifier<TerrestrialStepSoundGenerator> modifier) {
        this.entity = entity;
        this.engine = engine;
        this.modifier = modifier;
        this.associations = new AssociationPool(entity, engine);
    }

    @Override
    public float getLocalPitch(float tickDelta) {
        return biomePitch.get(tickDelta);
    }

    @Override
    public float getLocalVolume(float tickDelta) {
        return biomeVolume.get(tickDelta);
    }

    @Override
    public MotionTracker getMotionTracker() {
        return motionTracker;
    }

    @Override
    public void generateFootsteps() {
        BiomeVarianceLookup.BiomeVariance variance = entity.level().getBiome(entity.blockPosition()).unwrapKey().map(ResourceKey::identifier).map(key -> {
            return engine.getIsolator().biomes().lookup(key);
        }).orElse(BiomeVarianceLookup.BiomeVariance.DEFAULT);

        biomePitch.update(variance.pitch(), 0.01F);
        biomeVolume.update(variance.volume(), 0.01F);

        motionTracker.simulateMotionData(entity);
        simulateFootsteps();
        simulateAirborne();
        simulateBrushes();
        simulateStationary();
        lastFallDistance = motionTracker.getFallDistance();
    }

    protected void simulateStationary() {
        if (isImmobile && (entity.onGround() || !entity.isInWater()) && playbackImmobile()) {
            Association assos = associations.findAssociation(0d, isRightFoot);

            if (assos.isResult() && (!assos.isSilent() || !isImmobile)) {
                playStep(assos, State.STAND);
            }
        }
    }

    protected boolean playbackImmobile() {
        long now = System.currentTimeMillis();
        Variator variator = engine.getIsolator().variator();
        if (now - immobilePlayback > immobileInterval) {
            immobilePlayback = now;
            immobileInterval = (int) Math.floor(
                    (Math.random() * (variator.IMOBILE_INTERVAL_MAX - variator.IMOBILE_INTERVAL_MIN)) + variator.IMOBILE_INTERVAL_MIN);
            return true;
        }
        return false;
    }

    protected boolean updateImmobileState(float reference) {
        float diff = lastReference - reference;
        lastReference = reference;
        if (!isImmobile && diff == 0f) {
            timeImmobile = System.currentTimeMillis();
            isImmobile = true;
        } else if (isImmobile && diff != 0f) {
            isImmobile = false;
            return System.currentTimeMillis() - timeImmobile > engine.getIsolator().variator().IMMOBILE_DURATION;
        }

        return false;
    }

    protected void simulateFootsteps() {
        final float distanceReference = motionTracker.getDistanceTraveled();

        stepThisFrame = false;

        if (dmwBase > distanceReference) {
            dmwBase = 0;
            dwmYChange = 0;
        }

        double movX = motionTracker.getMotionX();
        double movZ = motionTracker.getMotionZ();

        double scal = movX * xMovec + movZ * zMovec;
        if (scalStat != scal < 0.001f) {
            scalStat = !scalStat;

            if (scalStat && engine.getIsolator().variator().PLAY_WANDER && !hasStoppingConditions()) {
                playStep(associations.findAssociation(0, isRightFoot), State.WANDER);
            }
        }
        xMovec = movX;
        zMovec = movZ;

        float dwm = distanceReference - dmwBase;
        boolean immobile = updateImmobileState(distanceReference);
        boolean climbing = entity.getLastClimbablePos().isPresent();
        if (immobile && !climbing) {
            dwm = 0;
            dmwBase = distanceReference;
        }

        if (entity.onGround() || entity.isUnderWater() || climbing) {
            State event = null;

            float distance = 0f;
            double verticalOffsetAsMinus = 0f;
            Variator variator = engine.getIsolator().variator();

            if (climbing && !entity.onGround()) {
                distance = variator.DISTANCE_LADDER;
            } else if (!entity.isUnderWater() && Math.abs(yPosition - entity.getY()) > 0.4) {
                // This ensures this does not get recorded as landing, but as a step
                if (yPosition < entity.getY()) { // Going upstairs
                    distance = variator.DISTANCE_STAIR;
                    event = motionTracker.pickState(entity, State.UP, State.UP_RUN);
                } else if (!entity.isSuppressingSlidingDownLadder()) { // Going downstairs
                    distance = -1f;
                    verticalOffsetAsMinus = 0f;
                    event = motionTracker.pickState(entity, State.DOWN, State.DOWN_RUN);
                }

                dwmYChange = distanceReference;
            } else {
                distance = variator.DISTANCE_HUMAN;
            }

            if (event == null) {
                event = motionTracker.pickState(entity, State.WALK, State.RUN);
            }

            // Fix high speed footsteps (i.e. horse riding)
            if ((entity instanceof AbstractHorse) && motionTracker.getHorizontalSpeed() > 0.1) {
                distance *= 3;
            }

            distance = modifier.reevaluateDistance(event, distance);
            // if the player is larger than normal, slow down footsteps further

            distance *= ((PlayerUtil.getScale(entity) - 1) * 0.6F) + 1;

            if (dwm > distance) {
                produceStep(event, verticalOffsetAsMinus);
                modifier.stepped(this, entity, event);
                dmwBase = distanceReference;
            }
        }

        if (entity.onGround()) {
            // This fixes an issue where the value is evaluated while the player is between
            // two steps in the air while descending stairs
            yPosition = entity.getY();
        }
    }

    public final void produceStep(@Nullable State event) {
        produceStep(event, 0d);
    }

    public final void produceStep(@Nullable State event, double verticalOffsetAsMinus) {

        if (event == null) {
            event = motionTracker.pickState(entity, State.WALK, State.RUN);
        }

        if (hasStoppingConditions()) {
            float volume = Math.min(1, (float) entity.getDeltaMovement().length() * 0.35F);

            engine.getIsolator().acoustics().playAcoustic(entity,
                    entity.isInWater() ? SoundsKey.SWIM_WATER : SoundsKey.SWIM_LAVA,
                    (entity.isUnderWater() || entity.isInLava()) ? State.SWIM : event,
                    Options.singular("gliding_volume", volume)
                    .and(Options.singular("volume_scale", PlayerUtil.isClientPlayer(entity) ? 1 : 0.125F))
            );
            playStep(associations.findAssociation(entity.blockPosition().below(), Solver.MESSY_FOLIAGE_STRATEGY), event);
        } else {
            if (!entity.isDiscrete() || event.isExtraLoud()) {
                playStep(associations.findAssociation(verticalOffsetAsMinus, isRightFoot), event);
            }
            isRightFoot = !isRightFoot;
        }

        stepThisFrame = true;
    }

    protected boolean hasStoppingConditions() {
        return entity.isInWater() || entity.isInLava();
    }

    protected void simulateAirborne() {
        if ((entity.onGround() || entity.getLastClimbablePos().isPresent()) == isAirborne) {
            isAirborne = !isAirborne;
            simulateJumpingLanding();
        }
    }

    protected boolean isJumping() {
        return ((ILivingEntity) entity).isJumping();
    }

    protected double getOffsetMinus() {
        if (entity instanceof RemotePlayer) {
            return 1;
        }
        return 0;
    }

    protected void simulateJumpingLanding() {
        if (hasStoppingConditions()) {
            return;
        }

        if (isAirborne && isJumping()) {
            simulateJumping();
        } else if (!isAirborne) {
            simulateLanding();
        }
    }

    protected void simulateJumping() {
        Variator variator = engine.getIsolator().variator();
        if (variator.EVENT_ON_JUMP) {
            if (motionTracker.getHorizontalSpeed() < variator.SPEED_TO_JUMP_AS_MULTIFOOT) {
                // STILL JUMP
                playMultifoot(getOffsetMinus() + 0.4d, State.WANDER);
                // 2 - 0.7531999805212d (magic number for vertical offset?)
            } else {
                playSinglefoot(getOffsetMinus() + 0.4d, State.JUMP, isRightFoot);
                // RUNNING JUMP
                // Do not toggle foot:
                // After landing sounds, the first foot will be same as the one used to jump.
            }
        }
    }

    protected void simulateLanding() {
        Variator variator = engine.getIsolator().variator();

        if (lastFallDistance > 0) {
            if (lastFallDistance > variator.LAND_HARD_DISTANCE_MIN) {
                playMultifoot(getOffsetMinus(), State.LAND);
                // Always assume the player lands on their two feet
                // Do not toggle foot:
                // After landing sounds, the first foot will be same as the one used to jump.
            } else if (!stepThisFrame && !entity.isDiscrete()) {
                playSinglefoot(getOffsetMinus(), motionTracker.pickState(entity, State.CLIMB, State.CLIMB_RUN), isRightFoot);
                if (!stepThisFrame) {
                    isRightFoot = !isRightFoot;
                }
            }
        }
    }

    private void simulateBrushes() {
        if (brushesTime > System.currentTimeMillis()) {
            return;
        }

        brushesTime = System.currentTimeMillis() + 100;

        if (motionTracker.isStationary() || entity.isDiscrete() || !entity.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
            return;
        }

        Association assos = associations.findAssociation(BlockPos.containing(
            entity.getX(),
            MoreObjects.firstNonNull(entity.getRootVehicle(), entity).getY() - 0.1D - (entity.onGround() ? 0 : 0.25D),
            entity.getZ()
        ), Solver.MESSY_FOLIAGE_STRATEGY);

        if (!assos.isSilent()) {
            if (!isMessyFoliage) {
                isMessyFoliage = true;
                playStep(assos, State.WALK);
            }
        } else if (isMessyFoliage) {
            isMessyFoliage = false;
        }
    }

    protected void playStep(Association association, State eventType) {
        if (engine.getConfig().getEnabledFootwear()) {

            Equippable equipable = entity.getItemBySlot(EquipmentSlot.FEET).get(DataComponents.EQUIPPABLE);
            if (equipable != null) {
                SoundsKey bootSound = engine.getIsolator().primitives().getAssociation(equipable.equipSound().value(), Substrates.DEFAULT);
                if (bootSound.isEmitter()) {
                    engine.getIsolator().acoustics().playStep(association, eventType, Options.singular("volume_percentage", 0.5F));
                    engine.getIsolator().acoustics().playAcoustic(entity, bootSound, eventType, Options.EMPTY);
                    return;
                }
            }
        }

        engine.getIsolator().acoustics().playStep(association, eventType, Options.EMPTY);
    }

    protected void playSinglefoot(double verticalOffsetAsMinus, State eventType, boolean foot) {
        Association assos = associations.findAssociation(verticalOffsetAsMinus, isRightFoot);

        if (!assos.isResult()) {
            assos = associations.findAssociation(verticalOffsetAsMinus + 1, isRightFoot);
        }

        playStep(assos, eventType);
    }

    protected void playMultifoot(double verticalOffsetAsMinus, State eventType) {
        // STILL JUMP
        Association leftFoot = associations.findAssociation(verticalOffsetAsMinus, false);
        Association rightFoot = associations.findAssociation(verticalOffsetAsMinus, true);

        if (leftFoot.isResult() && leftFoot.dataEquals(rightFoot)) {
            // If the two feet solve to the same sound, except NO_ASSOCIATION, only play the sound once
            if (isRightFoot) {
                leftFoot = Association.NOT_EMITTER;
            } else {
                rightFoot = Association.NOT_EMITTER;
            }
        }

        playStep(leftFoot, eventType);
        playStep(rightFoot, eventType);
    }
}
