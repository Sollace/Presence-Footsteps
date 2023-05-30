package eu.ha3.presencefootsteps.sound.acoustics;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.world.Association;
import net.minecraft.entity.LivingEntity;

public interface AcousticLibrary {
    /**
     * Adds an acoustic to the library.
     */
    void addAcoustic(String name, Acoustic acoustic);

    /**
     * Plays an acoustic with additional options.
     */
    default void playAcoustic(Association association, State event, Options options) {
        playAcoustic(association.getSource(), association.getAcousticName(), event, options);
        if (Options.WET_VOLUME_OPTIONS.get("volume_percentage") > 0.1F) {
            playAcoustic(association.getSource(), association.getWetAcousticName(), event, options.and(Options.WET_VOLUME_OPTIONS));
        }
    }

    void playAcoustic(LivingEntity location, String acousticName, State event, Options options);
}