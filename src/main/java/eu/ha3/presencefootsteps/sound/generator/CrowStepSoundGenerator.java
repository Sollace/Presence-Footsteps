package eu.ha3.presencefootsteps.sound.generator;

import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.util.MathUtil;
import eu.ha3.presencefootsteps.sound.Options;
import net.minecraft.entity.LivingEntity;
class CrowStepSoundGenerator extends BipedalStepSoundGenerator {

    protected boolean isFalling = false;

    protected FlightState state = FlightState.IDLE;
    protected int flapMod = 0;
    private long lastTimeImmobile;
    protected long nextFlapTime;

    @Override
    public boolean generateFootsteps(LivingEntity ply) {
        lastTimeImmobile = timeImmobile;
        return super.generateFootsteps(ply);
    }

    @Override
    protected void simulateAirborne(LivingEntity ply) {
        isFalling = motionY < -0.3;
        super.simulateAirborne(ply);
        if (isAirborne) {
            simulateFlying(ply);
        }
    }

    protected boolean updateState(double x, double y, double z, double strafe) {
        double smotionHor = x * x + z * z;
        float motionHor = (float) Math.sqrt(smotionHor);
        FlightState result = FlightState.IDLE;
        if (motionHor > variator.MIN_DASH_MOTION) {
            result = FlightState.DASHING;
        } else if (motionHor > variator.MIN_COAST_MOTION && (float) Math.abs(y) < variator.MIN_COAST_MOTION / 20) {
            if (strafe > variator.MIN_MOTION_Y) {
                result = FlightState.COASTING_STRAFING;
            } else {
                result = FlightState.COASTING;
            }
        } else if (motionHor > variator.MIN_MOTION_HOR) {
            result = FlightState.FLYING;
        } else if (y < 0) {
            result = FlightState.DESCENDING;
        } else if ((float) y > variator.MIN_MOTION_Y) {
            result = FlightState.ASCENDING;
        }
        boolean changed = result != state;
        state = result;
        return changed;
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

        double xpd = motionX * motionX + motionZ * motionZ;
        float speed = (float) Math.sqrt(xpd);

        if (isAirborne) {
            nextFlapTime = now + variator.WING_JUMPING_REST_TIME;
        }

        boolean hugeLanding = !isAirborne && fallDistance > variator.HUGEFALL_LANDING_DISTANCE_MIN;
        boolean speedingJumpStateChange = speed > variator.MIN_MOTION_HOR;

        if (hugeLanding || speedingJumpStateChange) {
            if (!isAirborne) {
                float volume = speedingJumpStateChange ? 2
                        : MathUtil.scalex(fallDistance, variator.HUGEFALL_LANDING_DISTANCE_MIN, variator.HUGEFALL_LANDING_DISTANCE_MAX);
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

        if (updateState(motionX, motionY, motionZ, ply.sidewaysSpeed)) {
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
