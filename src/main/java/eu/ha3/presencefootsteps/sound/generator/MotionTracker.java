package eu.ha3.presencefootsteps.sound.generator;

import eu.ha3.presencefootsteps.config.Variator;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.util.PlayerUtil;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class MotionTracker {
    private double lastX;
    private double lastY;
    private double lastZ;

    protected double motionX;
    protected double motionY;
    protected double motionZ;

    protected float distanceTraveled;
    protected double fallDistance;

    private final TerrestrialStepSoundGenerator generator;

    public MotionTracker(TerrestrialStepSoundGenerator generator) {
        this.generator = generator;
    }

    public double getMotionX() {
        return motionX;
    }

    public double getMotionY() {
        return motionY;
    }

    public double getMotionZ() {
        return motionZ;
    }

    public double getHorizontalSpeed() {
        return motionX * motionX + motionZ * motionZ;
    }

    public boolean isStationary() {
        return motionX == 0 && motionZ == 0;
    }

    public float getDistanceTraveled() {
        return distanceTraveled;
    }

    public double getFallDistance() {
        return fallDistance;
    }

    /**
     * Fills in the blanks that aren't present on the client when playing on a
     * remote server.
     */
    public void simulateMotionData(LivingEntity ply) {
        if (PlayerUtil.isClientPlayer(ply)) {
            motionX = ply.getDeltaMovement().x;
            motionY = ply.getDeltaMovement().y;
            motionZ = ply.getDeltaMovement().z;
            distanceTraveled = ply.moveDist;
            fallDistance = ply.fallDistance;
        } else {
            // Other players don't send their motion data so we have to make our own
            // approximations.
            motionX = (ply.getX() - lastX);
            lastX = ply.getX();
            motionY = (ply.getY() - lastY);

            if (ply.onGround()) {
                motionY += 0.0784000015258789d;
            }

            lastY = ply.getY();

            motionZ = (ply.getZ() - lastZ);
            lastZ = ply.getZ();
        }

        if (ply instanceof RemotePlayer other) {
            if (ply.level().getGameTime() % 1 == 0) {

                if (motionX != 0 || motionZ != 0) {
                    distanceTraveled += Math.sqrt(Math.pow(motionX, 2) + Math.pow(motionY, 2) + Math.pow(motionZ, 2)) * 0.8;
                } else {
                    distanceTraveled += Math.sqrt(Math.pow(motionX, 2) + Math.pow(motionZ, 2)) * 0.8;
                }

                if (ply.onGround() || ply.isPassenger() || other.getAbilities().flying || motionY > 0) {
                    fallDistance = 0;
                } else if (motionY < 0) {
                    fallDistance -= motionY;
                }
            }
        }

        if (!(ply instanceof Player)) {
            distanceTraveled += (float)Math.sqrt(getHorizontalSpeed()) * 0.6f;
        }
    }

    public State pickState(LivingEntity ply, State walk, State run) {
        if (ply instanceof Player) {
            if (!PlayerUtil.isClientPlayer(ply)) {
                // Other players don't send motion data, so have to decide some other way
                if (ply.isSprinting()) {
                    return run;
                }
                return walk;
            }
        }
        return getHorizontalSpeed() > generator.engine.getIsolator().variator().SPEED_TO_RUN ? run : walk;
    }

    public float getSpeedScalingRatio(LivingEntity entity) {
        Variator variator = generator.engine.getIsolator().variator();
        variator.RUNNING_RAMPUP_BEGIN = 0.011F;
        variator.RUNNING_RAMPUP_END = 0.022F;
        double relativeSpeed = getHorizontalSpeed() + (getMotionY() * getMotionY()) - variator.RUNNING_RAMPUP_BEGIN;
        double maxSpeed = variator.RUNNING_RAMPUP_END - variator.RUNNING_RAMPUP_BEGIN;
        return (float)Mth.clamp(relativeSpeed / maxSpeed, 0, 1);
    }
}
