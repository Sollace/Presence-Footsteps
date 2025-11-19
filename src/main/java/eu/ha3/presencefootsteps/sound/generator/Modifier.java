package eu.ha3.presencefootsteps.sound.generator;

import eu.ha3.presencefootsteps.sound.State;
import net.minecraft.world.entity.LivingEntity;

public class Modifier<T extends StepSoundGenerator> {
    protected void stepped(T generator, LivingEntity ply, State event) {

    }

    protected float reevaluateDistance(State event, float distance) {
        return distance;
    }
}
