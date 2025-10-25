package eu.ha3.presencefootsteps.util;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

public final class Edge implements BooleanConsumer {

    private boolean state;
    private final BooleanConsumer callback;

    public Edge(BooleanConsumer callback) {
        this.callback = callback;
    }

    @Override
    public void accept(boolean newState) {
        if (state != newState) {
            state = newState;
            callback.accept(newState);
        }
    }
}
