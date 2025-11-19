package eu.ha3.presencefootsteps.util;

import net.minecraft.util.Mth;

public class Lerp {
    public float previous;
    public float current;

    public void update(float newTarget, float rate) {
        previous = current;
        if (current < newTarget) {
            current = Math.min(current + rate, newTarget);
        }
        if (current > newTarget) {
            current = Math.max(current - rate, newTarget);
        }
    }

    public float get(float tickDelta) {
        return Mth.lerp(tickDelta, previous, current);
    }
}
