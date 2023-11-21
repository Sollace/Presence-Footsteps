package eu.ha3.presencefootsteps.sound.generator;

import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

import org.jetbrains.annotations.Nullable;

import eu.ha3.presencefootsteps.config.Variator;
import eu.ha3.presencefootsteps.mixins.ILivingEntity;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.acoustics.AcousticLibrary;
import eu.ha3.presencefootsteps.util.PlayerUtil;
import eu.ha3.presencefootsteps.sound.Isolator;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.world.Association;
import eu.ha3.presencefootsteps.world.Solver;

class TerrestrialStepSoundGenerator implements StepSoundGenerator {
    // Construct
    protected Solver solver;
    protected AcousticLibrary acoustics;
    protected Variator variator;

    protected final MotionTracker motionTracker = new MotionTracker(this);

    // Footsteps
    protected float dmwBase;
    protected float dwmYChange;
    protected double yPosition;

    // Airborne
    protected boolean isAirborne;

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

    private final Modifier<TerrestrialStepSoundGenerator> modifier;

    private boolean playedSound;

    private int ticksInactive;


    public TerrestrialStepSoundGenerator(Modifier<TerrestrialStepSoundGenerator> modifier) {
        this.modifier = modifier;
    }

    @Override
    public boolean isInactive() {
        return ticksInactive > 23;
    }

    @Override
    public void setIsolator(Isolator isolator) {
        solver = isolator.getSolver();
        acoustics = isolator.getAcoustics();
        variator = isolator.getVariator();
    }

    @Override
    public MotionTracker getMotionTracker() {
        return motionTracker;
    }

    @Override
    public boolean generateFootsteps(LivingEntity ply) {
        motionTracker.simulateMotionData(ply);
        playedSound = motionTracker.isStationary();
        simulateFootsteps(ply);
        simulateAirborne(ply);
        simulateBrushes(ply);
        simulateStationary(ply);
        if (!playedSound) {
            ticksInactive++;
        } else {
            ticksInactive = 0;
        }
        return !isInactive();
    }

    protected void simulateStationary(LivingEntity ply) {
        if (isImmobile && (ply.isOnGround() || !ply.isSubmergedInWater()) && playbackImmobile()) {
            Association assos = solver.findAssociation(ply, 0d, isRightFoot);

            if (assos.hasAssociation() || !isImmobile) {
                playedSound |= solver.playAssociation(ply, assos, State.STAND);
            }
        }
    }

    protected boolean playbackImmobile() {
        long now = System.currentTimeMillis();
        if (now - immobilePlayback > immobileInterval) {
            immobilePlayback = now;
            immobileInterval = (int) Math.floor(
                    (Math.random() * (variator.IMOBILE_INTERVAL_MAX - variator.IMOBILE_INTERVAL_MIN)) + variator.IMOBILE_INTERVAL_MIN);
            return true;
        }
        return false;
    }

    protected boolean updateImmobileState(LivingEntity ply, float reference) {
        float diff = lastReference - reference;
        lastReference = reference;
        if (!isImmobile && diff == 0f) {
            timeImmobile = System.currentTimeMillis();
            isImmobile = true;
        } else if (isImmobile && diff != 0f) {
            isImmobile = false;
            return System.currentTimeMillis() - timeImmobile > variator.IMMOBILE_DURATION;
        }

        return false;
    }

    protected void simulateFootsteps(LivingEntity ply) {
        final float distanceReference = ply.distanceTraveled;

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

            if (scalStat && variator.PLAY_WANDER && !hasStoppingConditions(ply)) {
                playedSound |= solver.playAssociation(ply, solver.findAssociation(ply, 0, isRightFoot), State.WANDER);
            }
        }
        xMovec = movX;
        zMovec = movZ;

        float dwm = distanceReference - dmwBase;
        boolean immobile = updateImmobileState(ply, distanceReference);
        if (immobile && !ply.isClimbing()) {
            dwm = 0;
            dmwBase = distanceReference;
        }

        if (ply.isOnGround() || ply.isSubmergedInWater() || ply.isClimbing()) {
            State event = null;

            float distance = 0f;
            double verticalOffsetAsMinus = 0f;

            if (ply.isClimbing() && !ply.isOnGround()) {
                distance = variator.DISTANCE_LADDER;
            } else if (!ply.isSubmergedInWater() && Math.abs(yPosition - ply.getY()) > 0.4) {
                // This ensures this does not get recorded as landing, but as a step
                if (yPosition < ply.getY()) { // Going upstairs
                    distance = variator.DISTANCE_STAIR;
                    event = motionTracker.pickState(ply, State.UP, State.UP_RUN);
                } else if (!ply.isSneaking()) { // Going downstairs
                    distance = -1f;
                    verticalOffsetAsMinus = 0f;
                    event = motionTracker.pickState(ply, State.DOWN, State.DOWN_RUN);
                }

                dwmYChange = distanceReference;
            } else {
                distance = variator.DISTANCE_HUMAN;
            }

            if (event == null) {
                event = motionTracker.pickState(ply, State.WALK, State.RUN);
            }
            distance = modifier.reevaluateDistance(event, distance);
            // if the player is larger than normal, slow down footsteps further

            distance *= ((PlayerUtil.getScale(ply) - 1) * 0.6F) + 1;

            if (dwm > distance) {
                produceStep(ply, event, verticalOffsetAsMinus);
                modifier.stepped(this, ply, event);
                dmwBase = distanceReference;
            }
        }

        if (ply.isOnGround()) {
            // This fixes an issue where the value is evaluated while the player is between
            // two steps in the air while descending stairs
            yPosition = ply.getY();
        }
    }

    public final void produceStep(LivingEntity ply, @Nullable State event) {
        produceStep(ply, event, 0d);
    }

    public final void produceStep(LivingEntity ply, @Nullable State event, double verticalOffsetAsMinus) {

        if (event == null) {
            event = motionTracker.pickState(ply, State.WALK, State.RUN);
        }

        if (hasStoppingConditions(ply)) {
            float volume = Math.min(1, (float) ply.getVelocity().length() * 0.35F);
            Options options = Options.singular("gliding_volume", volume);
            State state = ply.isSubmergedInWater() ? State.SWIM : event;

            acoustics.playAcoustic(ply, "_SWIM", state, options);

            playedSound |= solver.playAssociation(ply, solver.findAssociation(ply, ply.getBlockPos().down(), Solver.MESSY_FOLIAGE_STRATEGY), event);
        } else {
            if (!ply.isSneaky() || event.isExtraLoud()) {
                playedSound |= solver.playAssociation(ply, solver.findAssociation(ply, verticalOffsetAsMinus, isRightFoot), event);
            }
            isRightFoot = !isRightFoot;
        }

        stepThisFrame = true;
    }

    protected boolean hasStoppingConditions(Entity ply) {
        return ply.isTouchingWater();
    }

    protected void simulateAirborne(LivingEntity ply) {
        if ((ply.isOnGround() || ply.isClimbing()) == isAirborne) {
            isAirborne = !isAirborne;
            simulateJumpingLanding(ply);
        }
    }

    protected boolean isJumping(LivingEntity ply) {
        return ((ILivingEntity) ply).isJumping();
    }

    protected double getOffsetMinus(LivingEntity ply) {
        if (ply instanceof OtherClientPlayerEntity) {
            return 1;
        }
        return 0;
    }

    protected void simulateJumpingLanding(LivingEntity ply) {
        if (hasStoppingConditions(ply)) {
            return;
        }

        if (isAirborne && isJumping(ply)) {
            simulateJumping(ply);
        } else if (!isAirborne) {
            simulateLanding(ply);
        }
    }

    protected void simulateJumping(LivingEntity ply) {
        if (variator.EVENT_ON_JUMP) {
            if (motionTracker.getHorizontalSpeed() < variator.SPEED_TO_JUMP_AS_MULTIFOOT) {
                // STILL JUMP
                playMultifoot(ply, getOffsetMinus(ply) + 0.4d, State.WANDER);
                // 2 - 0.7531999805212d (magic number for vertical offset?)
            } else {
                playSinglefoot(ply, getOffsetMinus(ply) + 0.4d, State.JUMP, isRightFoot);
                // RUNNING JUMP
                // Do not toggle foot:
                // After landing sounds, the first foot will be same as the one used to jump.
            }
        }
    }

    protected void simulateLanding(LivingEntity ply) {
        if (ply.fallDistance > 0) {
            if (ply.fallDistance > variator.LAND_HARD_DISTANCE_MIN) {
                playMultifoot(ply, getOffsetMinus(ply), State.LAND);
                // Always assume the player lands on their two feet
                // Do not toggle foot:
                // After landing sounds, the first foot will be same as the one used to jump.
            } else if (/* !this.stepThisFrame &&*/ !ply.isSneaking()) {
                playSinglefoot(ply, getOffsetMinus(ply), motionTracker.pickState(ply, State.CLIMB, State.CLIMB_RUN), isRightFoot);
                if (!this.stepThisFrame)
                    isRightFoot = !isRightFoot;
            }
        }
    }

    private void simulateBrushes(LivingEntity ply) {
        if (brushesTime > System.currentTimeMillis()) {
            return;
        }

        brushesTime = System.currentTimeMillis() + 100;

        if (motionTracker.isStationary() || ply.isSneaking()) {
            return;
        }

        Association assos = solver.findAssociation(ply, BlockPos.ofFloored(
            ply.getX(),
            ply.getY() - 0.1D - (ply.hasVehicle() ? ply.getRidingOffset(ply.getVehicle()) : 0) - (ply.isOnGround() ? 0 : 0.25D),
            ply.getZ()
        ), Solver.MESSY_FOLIAGE_STRATEGY);

        if (!assos.isNull()) {
            if (!isMessyFoliage) {
                isMessyFoliage = true;
                playedSound |= solver.playAssociation(ply, assos, State.WALK);
            }
        } else if (isMessyFoliage) {
            isMessyFoliage = false;
        }
    }

    protected void playSinglefoot(LivingEntity ply, double verticalOffsetAsMinus, State eventType, boolean foot) {
        Association assos = solver.findAssociation(ply, verticalOffsetAsMinus, isRightFoot);

        if (assos.isNotEmitter()) {
            assos = solver.findAssociation(ply, verticalOffsetAsMinus + 1, isRightFoot);
        }

        playedSound |= solver.playAssociation(ply, assos, eventType);
    }

    protected void playMultifoot(LivingEntity ply, double verticalOffsetAsMinus, State eventType) {
        // STILL JUMP
        Association leftFoot = solver.findAssociation(ply, verticalOffsetAsMinus, false);
        Association rightFoot = solver.findAssociation(ply, verticalOffsetAsMinus, true);

        if (leftFoot.dataEquals(rightFoot)) {
            // If the two feet solve to the same sound, except NO_ASSOCIATION, only play the sound once
            if (isRightFoot) {
                leftFoot = Association.NOT_EMITTER;
            } else {
                rightFoot = Association.NOT_EMITTER;
            }
        }

        playedSound |= solver.playAssociation(ply, leftFoot, eventType);
        playedSound |= solver.playAssociation(ply, rightFoot, eventType);
    }
}
