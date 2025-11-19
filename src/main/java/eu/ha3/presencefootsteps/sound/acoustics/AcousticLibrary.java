package eu.ha3.presencefootsteps.sound.acoustics;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.world.Association;
import eu.ha3.presencefootsteps.world.SoundsKey;
import net.minecraft.world.entity.LivingEntity;

public interface AcousticLibrary {
    /**
     * Adds an acoustic to the library.
     */
    void addAcoustic(String name, Acoustic acoustic);

    void playStep(Association assos, State eventType, Options options);

    void playAcoustic(LivingEntity location, SoundsKey acousticName, State event, Options options);

    void think();
}