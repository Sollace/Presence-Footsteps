package eu.ha3.presencefootsteps.sound.generator;

import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.util.MathUtil;
import eu.ha3.presencefootsteps.world.SoundsKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import eu.ha3.presencefootsteps.config.Variator;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.SoundEngine;

class WingedStepSoundGenerator extends TerrestrialStepSoundGenerator {
    private static final SoundsKey SWIFT = SoundsKey.of("swift");
    private static final SoundsKey WING = SoundsKey.of("wing");
    protected boolean isFalling = false;

    protected FlightState state = FlightState.IDLE;
    protected int flapMod = 0;
    private long lastTimeImmobile;
    protected long nextFlapTime;

    public WingedStepSoundGenerator(LivingEntity entity, SoundEngine engine, Modifier<TerrestrialStepSoundGenerator> modifier) {
        super(entity, engine, modifier);
    }

    @Override
    public void generateFootsteps() {
        lastTimeImmobile = timeImmobile;
        super.generateFootsteps();
    }

    @Override
    protected void simulateAirborne() {
        isFalling = motionTracker.getMotionY() < -0.3;
        super.simulateAirborne();
        if (isAirborne) {
            simulateFlying();
        }
    }

    @Override
    protected boolean updateImmobileState(float reference) {

        if (isAirborne) {
            final Vec3 vel = entity.getDeltaMovement();

            boolean stationary = vel.x != 0 && vel.z != 0;
            lastReference = reference;
            if (!isImmobile && stationary) {
                timeImmobile = System.currentTimeMillis();
                isImmobile = true;
            } else if (isImmobile && !stationary) {
                isImmobile = false;
                return System.currentTimeMillis() - timeImmobile > engine.getIsolator().variator().IMMOBILE_DURATION;
            }

            return false;
        }

        return super.updateImmobileState(reference);
    }

    protected int getWingSpeed() {
        Variator variator = engine.getIsolator().variator();
        return switch (state) {
            case COASTING -> flapMod == 0
                    ? variator.WING_SPEED_COAST
                    : variator.WING_SPEED_NORMAL * flapMod;
            case COASTING_STRAFING -> variator.WING_SPEED_NORMAL * (1 + flapMod);
            case DASHING -> variator.WING_SPEED_RAPID;
            case ASCENDING, FLYING -> variator.WING_SPEED_NORMAL;
            default -> variator.WING_SPEED_IDLE;
        };
    }

    @Override
    protected void simulateJumpingLanding() {
        if (hasStoppingConditions()) {
            return;
        }

        final long now = System.currentTimeMillis();

        float speed = (float) Math.sqrt(motionTracker.getHorizontalSpeed());
        Variator variator = engine.getIsolator().variator();

        if (isAirborne) {
            nextFlapTime = now + variator.WING_JUMPING_REST_TIME;
        }

        boolean hugeLanding = !isAirborne && lastFallDistance > variator.HUGEFALL_LANDING_DISTANCE_MIN;
        boolean speedingJumpStateChange = speed > variator.MIN_MOTION_HOR;

        if (hugeLanding || speedingJumpStateChange) {
            if (!isAirborne) {
                float volume = speedingJumpStateChange ? 2
                        : MathUtil.scalex((float)lastFallDistance, variator.HUGEFALL_LANDING_DISTANCE_MIN, variator.HUGEFALL_LANDING_DISTANCE_MAX);
                engine.getIsolator().acoustics().playAcoustic(entity, SWIFT, State.LAND, Options.singular("gliding_volume", volume));
            } else {
                engine.getIsolator().acoustics().playAcoustic(entity, SWIFT, State.JUMP, Options.EMPTY);
            }
        }

        if (isAirborne && isJumping()) {
            simulateJumping();
        } else if (!isAirborne && hugeLanding) {
            simulateLanding();
        }
    }

    protected void simulateFlying() {
        final long now = System.currentTimeMillis();
        Variator variator = engine.getIsolator().variator();

        if (updateState(motionTracker.getHorizontalSpeed(), motionTracker.getMotionY(), entity.xxa)) {
            nextFlapTime = now + variator.FLIGHT_TRANSITION_TIME;
        }

        if (!entity.isUnderWater() && !isFalling && now > nextFlapTime) {
            nextFlapTime = now + getWingSpeed() + (entity.level().random.nextInt(100) - 50);
            flapMod = (flapMod + 1) % (1 + entity.level().random.nextInt(4));

            float volume = 1;
            long diffImmobile = now - lastTimeImmobile;

            if (diffImmobile > variator.WING_IMMOBILE_FADE_START) {
                volume -= MathUtil.scalex(diffImmobile,
                        variator.WING_IMMOBILE_FADE_START,
                        variator.WING_IMMOBILE_FADE_START + variator.WING_IMMOBILE_FADE_DURATION);
            }

            engine.getIsolator().acoustics().playAcoustic(entity, WING, State.WALK, Options.singular("gliding_volume", volume));
        }
    }

    protected boolean updateState(double horSpeed, double verticalSpeed, double strafe) {
        float motionHor = (float) Math.sqrt(horSpeed);
        FlightState result = FlightState.IDLE;
        Variator variator = engine.getIsolator().variator();
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
