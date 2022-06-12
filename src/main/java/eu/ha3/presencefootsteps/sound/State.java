package eu.ha3.presencefootsteps.sound;

import org.jetbrains.annotations.Nullable;

public enum State {
    /**
     * Stationary. (no movement)
     */
    STAND(null),
    /**
     * Walking
     */
    WALK(null),
    /**
     * Stationary, changing direction
     */
    WANDER(null),
    /**
     * Swimming
     */
    SWIM(null),
    /**
     * Running
     */
    RUN(WALK),
    /**
     * Take off and landing whilst jumping
     */
    JUMP(WANDER),
    /**
     * Landing (after a fall) and jumping in place
     */
    LAND(RUN),
    /**
     * Climbing a ladder
     */
    CLIMB(WALK),
    /**
     * Climbing a ladder whilst running
     */
    CLIMB_RUN(RUN),
    /**
     * Descending stairs
     */
    DOWN(WALK),
    /**
     * Descending stairs whilst running
     */
    DOWN_RUN(RUN),
    /**
     * Ascending stairs
     */
    UP(WALK),
    /**
     * Ascending stairs whilst running
     */
    UP_RUN(RUN);

    private final State destination;

    private final String jsonName;

    State(@Nullable State dest) {
        destination = dest == null ? this : dest;
        jsonName = name().toLowerCase();
    }

    public String getName() {
        return jsonName;
    }

    public boolean canTransition() {
        return destination != this;
    }

    public State getTransitionDestination() {
        return destination;
    }
}
