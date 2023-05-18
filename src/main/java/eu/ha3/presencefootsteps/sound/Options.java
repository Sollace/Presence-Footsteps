package eu.ha3.presencefootsteps.sound;

import eu.ha3.presencefootsteps.PresenceFootsteps;

public interface Options {
    Options EMPTY = new Options() {
        @Override
        public boolean containsKey(String option) {
            return false;
        }

        @Override
        public float get(String option) {
            return 0;
        }
    };
    Options WET_VOLUME_OPTIONS = ofGetter("volume_percentage", () -> {
        return PresenceFootsteps.getInstance().getConfig().getWetSoundsVolume() / 100F;
    });

    static Options singular(String key, float value) {
        return ofGetter(key, () -> value);
    }

    static Options ofGetter(String key, FloatSupplier value) {
        return new Options() {
            @Override
            public boolean containsKey(String option) {
                return key.equals(option);
            }

            @Override
            public float get(String option) {
                return containsKey(option) ? value.get() : 0;
            }
        };
    }

    boolean containsKey(String option);

    float get(String option);

    default float getOrDefault(String option, float defaultValue) {
        return containsKey(option) ? get(option) : defaultValue;
    }

    default Options and(Options other) {
        final Options self = this;
        if (self == EMPTY) {
            return other;
        }
        return new Options() {

            @Override
            public boolean containsKey(String o) {
                return other.containsKey(o) || self.containsKey(o);
            }

            @Override
            public float get(String o) {
                return other.containsKey(o) ? other.get(o) : self.get(o);
            }
        };
    }

    interface FloatSupplier {
        float get();
    }
}
