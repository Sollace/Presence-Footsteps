package eu.ha3.presencefootsteps.sound;

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

    static Options singular(String key, float value) {
        return new Options() {
            @Override
            public boolean containsKey(String option) {
                return key.equals(option);
            }

            @Override
            public float get(String option) {
                return containsKey(option) ? value : 0;
            }
        };
    }

    boolean containsKey(String option);

    float get(String option);

}