package eu.ha3.presencefootsteps.sound.player;

import java.util.List;
import java.util.Random;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.util.MathUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.LivingEntity;

class DelayedSoundPlayer implements SoundPlayer {

    private static final boolean USING_LATENESS = true;
    private static final boolean USING_EARLYNESS = true;

    private static final float LATENESS_THRESHOLD = 1.5F;

    private static final double EARLYNESS_THRESHOLD_POW = 0.75D;

    private final List<PendingSound> pending = new ObjectArrayList<>();

    private final SoundPlayer immediate;

    private long nextPlayTime;
    private long currentTime;

    public DelayedSoundPlayer(SoundPlayer immediate) {
        this.immediate = immediate;
    }

    @Override
    public Random getRNG() {
        return immediate.getRNG();
    }

    @Override
    public void playSound(LivingEntity location, String soundName, float volume, float pitch, Options options) {
        pending.add(new PendingSound(location, soundName, volume, pitch, options));
    }

    @Override
    public void think() {
        currentTime = System.currentTimeMillis();

        if (pending.isEmpty() || currentTime < nextPlayTime) {
            return;
        }

        nextPlayTime = Long.MAX_VALUE;

        pending.removeIf(PendingSound::tick);
    }

    private class PendingSound {
        private final LivingEntity location;

        private final String soundName;

        private final float volume;
        private final float pitch;

        private final long timeToPlay;
        private final long maximum;

        public PendingSound(LivingEntity location, String soundName, float volume, float pitch, Options options) {
            this.location = location;
            this.soundName = soundName;
            this.volume = volume;
            this.pitch = pitch;

            maximum = options.containsKey("skippable") ? -1L : (long)options.get("delay_max");
            timeToPlay = System.currentTimeMillis() + Math.max(MathUtil.randAB(getRNG(),
                    (long)options.get("delay_min"),
                    (long)options.get("delay_max")
            ), nextPlayTime);
        }

        public boolean tick() {
            switch (nextState(currentTime)) {
            case PLAYING:
                immediate.playSound(location, soundName, volume, pitch, Options.EMPTY);
                return false;
            case SKIPPING:
                return true;
            default:
                nextPlayTime = timeToPlay;
                return false;
            }
        }

        private State nextState(long time) {
            if (time >= timeToPlay || USING_EARLYNESS
             && time >= timeToPlay - Math.pow(maximum, EARLYNESS_THRESHOLD_POW)) {
                if (USING_EARLYNESS && time < timeToPlay) {
                    PresenceFootsteps.logger.debug("Playing early sound (early by " + (timeToPlay - time) + "ms, tolerence is " + Math.pow(maximum, EARLYNESS_THRESHOLD_POW));
                }

                long lateness = time - timeToPlay;

                if (!USING_LATENESS
                        || maximum < 0
                        || lateness <= maximum / LATENESS_THRESHOLD) {
                    return State.PLAYING;
                }

                PresenceFootsteps.logger.debug("Skipped late sound (late by " + lateness + "ms, tolerence is " + maximum / LATENESS_THRESHOLD + "ms)");

                return State.SKIPPING;
            }

            return State.PENDING;
        }
    }

    enum State {
        PENDING,
        PLAYING,
        SKIPPING
    }
}
