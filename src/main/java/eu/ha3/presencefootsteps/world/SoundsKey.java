package eu.ha3.presencefootsteps.world;

import java.util.stream.Stream;

public record SoundsKey(String raw, String[] names) {
    public static final SoundsKey UNASSIGNED = new SoundsKey("UNASSIGNED", new String[0]);
    static final SoundsKey NON_EMITTER = new SoundsKey("NOT_EMITTER", new String[0]);
    static final SoundsKey MESSY_GROUND = new SoundsKey("MESSY_GROUND", new String[0]);

    public static final SoundsKey SWIM_WATER = of("_SWIM_WATER");
    public static final SoundsKey SWIM_LAVA = of("_SWIM_LAVA");
    public static final SoundsKey WATERFINE = of("waterfine");
    public static final SoundsKey LAVAFINE = of("lavafine");

    public static SoundsKey of(String names) {
        if (MESSY_GROUND.raw().equals(names)) {
            return MESSY_GROUND;
        }
        if (UNASSIGNED.raw().equals(names)) {
            return UNASSIGNED;
        }
        if (NON_EMITTER.raw().equals(names)) {
            return NON_EMITTER;
        }
        return new SoundsKey(names);
    }

    SoundsKey(String names) {
        this(names, Stream.of(names.split(","))
                .filter(s -> !s.isEmpty())
                .distinct()
                .toArray(String[]::new));
    }

    public boolean isResult() {
        return this != UNASSIGNED;
    }

    public boolean isSilent() {
        return this == NON_EMITTER;
    }

    public boolean isEmitter() {
        return !isSilent();
    }
}
