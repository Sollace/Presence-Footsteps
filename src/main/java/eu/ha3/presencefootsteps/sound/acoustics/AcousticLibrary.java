package eu.ha3.presencefootsteps.sound.acoustics;

import java.util.Map;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.world.Association;
import eu.ha3.presencefootsteps.world.SoundsKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public interface AcousticLibrary {
    boolean load(Map<Identifier, Acoustic> acoustics);

    void playStep(Association assos, State eventType, Options options);

    void playAcoustic(LivingEntity location, SoundsKey acousticName, State event, Options options);

    void think();
}