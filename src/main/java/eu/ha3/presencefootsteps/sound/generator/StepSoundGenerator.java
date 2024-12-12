package eu.ha3.presencefootsteps.sound.generator;

/**
 * Has the ability to generate footsteps based on a Player.
 *
 * @author Hurry
 */
public interface StepSoundGenerator {

    float getLocalPitch(float tickDelta);

    float getLocalVolume(float tickDelta);

    /**
     * Gets the motion tracker used to determine the direction and speed for an entity during simulation.
     */
    MotionTracker getMotionTracker();

    /**
     * Generate footsteps sounds of the Entity.
     */
    void generateFootsteps();
}
