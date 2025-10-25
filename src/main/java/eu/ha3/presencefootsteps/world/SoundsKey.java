package eu.ha3.presencefootsteps.world;

import java.util.Locale;
import java.util.stream.Stream;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

public record SoundsKey(String raw, String[] names) {
    private static final Interner<SoundsKey> INTERNER = Interners.newWeakInterner();

    public static final SoundsKey UNASSIGNED = INTERNER.intern(new SoundsKey("UNASSIGNED", new String[0]));
    public static final SoundsKey NON_EMITTER = INTERNER.intern(new SoundsKey("NOT_EMITTER", new String[0]));
    public static final SoundsKey MESSY_GROUND = INTERNER.intern(new SoundsKey("MESSY_GROUND", new String[0]));
    public static final SoundsKey VANILLA = INTERNER.intern(new SoundsKey("VANILLA", new String[0]));

    public static final SoundsKey SWIM_WATER = of("swim_water");
    public static final SoundsKey SWIM_LAVA = of("swim_lava");
    public static final SoundsKey WATERFINE = of("waterfine");
    public static final SoundsKey LAVAFINE = of("lavafine");

    public static SoundsKey of(String names) {

        if (MESSY_GROUND.raw().equalsIgnoreCase(names)) {
            return MESSY_GROUND;
        }
        if (UNASSIGNED.raw().equalsIgnoreCase(names)) {
            return UNASSIGNED;
        }
        if (NON_EMITTER.raw().equalsIgnoreCase(names)) {
            return NON_EMITTER;
        }
        if (VANILLA.raw().equalsIgnoreCase(names)) {
            return VANILLA;
        }
        return INTERNER.intern(new SoundsKey(names.toLowerCase(Locale.ROOT)));
    }

    SoundsKey(String names) {
        this(names, names.indexOf(',') == -1 ? new String[] { names } : Stream.of(names.split(","))
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

    public boolean isVanilla() {
        return this == VANILLA;
    }

    public boolean isEmitter() {
        return !isSilent();
    }
}
