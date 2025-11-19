package eu.ha3.presencefootsteps.sound.generator;

import java.util.Random;

import eu.ha3.presencefootsteps.sound.State;
import net.minecraft.world.entity.LivingEntity;

public class QuadrupedModifier extends Modifier<TerrestrialStepSoundGenerator> {
    //private static final int STEPPING_FUNCTION = 2;

    private int hoof = 0;

    private float nextWalkDistanceMultiplier = 0.05f;

    private final Random rand = new Random();

    @Override
    protected void stepped(TerrestrialStepSoundGenerator generator, LivingEntity ply, State event) {
        if (hoof == 0 || hoof == 2) {
            nextWalkDistanceMultiplier = rand.nextFloat();
        }

        hoof = (hoof + 1) % 3;

        if (event == State.WALK) {
            generator.produceStep(event);
        }
    }

    @Override
    protected float reevaluateDistance(State event, float distance) {

        if (event == State.WALK) {
            /*switch (STEPPING_FUNCTION) {
            case 0: {
                final float overallMultiplier = 1.5f;
                final float ndm = 0.425f + nextWalkDistanceMultiplier * 0.15f;

                if (hoof == 1 || hoof == 3) {
                    return distance * ndm * overallMultiplier;
                }
                return distance * (1 - ndm) * overallMultiplier;
            }
            case 1: {
                final float overallMultiplier = 1.4f;
                final float ndm = 0.5f;

                if (hoof == 1 || hoof == 3) {
                    return distance * (ndm + nextWalkDistanceMultiplier * ndm * 0.5f) * overallMultiplier;
                }
                return distance * (1 - ndm) * overallMultiplier;
            }
            case 2: {*/
                final float overallMultiplier = 1.85f / 2;
                final float ndm = 0.2f;

                float pond = nextWalkDistanceMultiplier;
                pond *= pond;
                pond *= ndm;
                if (hoof == 1 || hoof == 3) {
                    return distance * pond * overallMultiplier;
                }
                return distance * (1 - pond) * overallMultiplier;
            //}
            //}
        }

        if (event == State.RUN && hoof == 0) {
            return distance * 0.8f;
        }

        if (event == State.RUN) {
            return distance * 0.3f;
        }

        return distance;
    }
}
