package eu.ha3.presencefootsteps.sound.generator;

import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.util.MathUtil;
import eu.ha3.presencefootsteps.sound.Options;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

class WingedStepSoundGenerator extends TerrestrialStepSoundGenerator {

    protected boolean isFalling = false;

    protected FlightState state = FlightState.IDLE;
    protected int flapMod = 0;
    private long lastTimeImmobile;
    protected long nextFlapTime;

    public WingedStepSoundGenerator(Modifier<TerrestrialStepSoundGenerator> modifier) {
        super(modifier);
    }

    @Override
    public boolean generateFootsteps(LivingEntity ply) {
        lastTimeImmobile = timeImmobile;
        return super.generateFootsteps(ply);
    }

    @Override
    protected void simulateAirborne(LivingEntity ply) {
        isFalling = motionTracker.getMotionY() < -0.3;
        super.simulateAirborne(ply);
        if (isAirborne) {
            simulateFlying(ply);
        }
    }

    @Override
    protected boolean updateImmobileState(LivingEntity ply, float reference) {

        if (isAirborne) {
            final Vec3d vel = ply.getVelocity();

            boolean stationary = vel.x != 0 && vel.z != 0;
            lastReference = reference;
            if (!isImmobile && stationary) {
                timeImmobile = System.currentTimeMillis();
                isImmobile = true;
            } else if (isImmobile && !stationary) {
                isImmobile = false;
                return System.currentTimeMillis() - timeImmobile > variator.IMMOBILE_DURATION;
            }

            return false;
        }

        return super.updateImmobileState(ply, reference);
    }

    protected int getWingSpeed() {
        switch (state) {
        case COASTING:
            if (flapMod == 0)
                return variator.WING_SPEED_COAST;
            return variator.WING_SPEED_NORMAL * flapMod;
        case COASTING_STRAFING:
            return variator.WING_SPEED_NORMAL * (1 + flapMod);
        case DASHING:
            return variator.WING_SPEED_RAPID;
        case ASCENDING:
        case FLYING:
            return variator.WING_SPEED_NORMAL;
        default:
            return variator.WING_SPEED_IDLE;
        }
    }

    @Override
    protected void simulateJumpingLanding(LivingEntity ply) {
        if (hasStoppingConditions(ply)) {
            return;
        }

        final long now = System.currentTimeMillis();

        float speed = (float) Math.sqrt(motionTracker.getHorizontalSpeed());

        if (isAirborne) {
            nextFlapTime = now + variator.WING_JUMPING_REST_TIME;
        }

        boolean hugeLanding = !isAirborne && ply.fallDistance > variator.HUGEFALL_LANDING_DISTANCE_MIN;
        boolean speedingJumpStateChange = speed > variator.MIN_MOTION_HOR;

        if (hugeLanding || speedingJumpStateChange) {
            if (!isAirborne) {
                float volume = speedingJumpStateChange ? 2
                        : MathUtil.scalex(ply.fallDistance, variator.HUGEFALL_LANDING_DISTANCE_MIN, variator.HUGEFALL_LANDING_DISTANCE_MAX);
                acoustics.playAcoustic(ply, "_SWIFT", State.LAND, Options.singular("gliding_volume", volume));
            } else {
                acoustics.playAcoustic(ply, "_SWIFT", State.JUMP, Options.EMPTY);
            }
        }

        if (isAirborne && isJumping(ply)) {
            simulateJumping(ply);
        } else if (!isAirborne && hugeLanding) {
            simulateLanding(ply);
        }
    }

    protected void simulateFlying(LivingEntity ply) {
        final long now = System.currentTimeMillis();

        if (updateState(motionTracker.getHorizontalSpeed(), motionTracker.getMotionY(), ply.sidewaysSpeed)) {
            nextFlapTime = now + variator.FLIGHT_TRANSITION_TIME;
        }

        if (!ply.isSubmergedInWater() && !isFalling && now > nextFlapTime) {
            nextFlapTime = now + getWingSpeed() + (ply.world.random.nextInt(100) - 50);
            flapMod = (flapMod + 1) % (1 + ply.world.random.nextInt(4));

            float volume = 1;
            long diffImmobile = now - lastTimeImmobile;

            if (diffImmobile > variator.WING_IMMOBILE_FADE_START) {
                volume -= MathUtil.scalex(diffImmobile,
                        variator.WING_IMMOBILE_FADE_START,
                        variator.WING_IMMOBILE_FADE_START + variator.WING_IMMOBILE_FADE_DURATION);
            }

            acoustics.playAcoustic(ply, "_WING", State.WALK, Options.singular("gliding_volume", volume));
        }
    }

    protected boolean updateState(double horSpeed, double verticalSpeed, double strafe) {
        float motionHor = (float) Math.sqrt(horSpeed);
        FlightState result = FlightState.IDLE;
        if (motionHor > variator.MIN_DASH_MOTION) {
            result = FlightState.DASHING;
        } else if (motionHor > variator.MIN_COAST_MOTION && (float) Math.abs(verticalSpeed) < variator.MIN_COAST_MOTION / 20) {
            if (strafe > variator.MIN_MOTION_Y) {
                result = FlightState.COASTING_STRAFING;
            } else {
                result = FlightState.COASTING;
            }
        } else if (motionHor > variator.MIN_MOTION_HOR) {
            result = FlightState.FLYING;
        } else if (verticalSpeed < 0) {
            result = FlightState.DESCENDING;
        } else if ((float) verticalSpeed > variator.MIN_MOTION_Y) {
            result = FlightState.ASCENDING;
        }
        boolean changed = result != state;
        state = result;
        return changed;
    }
    private enum FlightState {
        DASHING,
        COASTING,
        COASTING_STRAFING,
        FLYING,
        IDLE,
        ASCENDING,
        DESCENDING
    }
}
