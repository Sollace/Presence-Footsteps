package eu.ha3.presencefootsteps.sound.player;

import java.util.Random;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import eu.ha3.presencefootsteps.util.PlayerUtil;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import eu.ha3.presencefootsteps.sound.StepSoundSource;
import eu.ha3.presencefootsteps.sound.generator.StepSoundGenerator;

/**
 * A Library that can also play sounds and default footsteps.
 *
 * @author Hurry
 */
public final class ImmediateSoundPlayer implements SoundPlayer {
    private final Random random = new Random();
    private final SoundEngine engine;

    public ImmediateSoundPlayer(SoundEngine engine) {
        this.engine = engine;
    }

    @Override
    public Random getRNG() {
        return random;
    }

    @Override
    public void playSound(LivingEntity location, String soundName, float volume, float pitch, Options options) {
        volume *= options.getOrDefault("volume_percentage", 1F);
        pitch *= options.getOrDefault("pitch_percentage", 1F);

        MinecraftClient mc = MinecraftClient.getInstance();
        double distance = mc.gameRenderer.getCamera().getCameraPos().squaredDistanceTo(location.getEntityPos());

        volume *= engine.getVolumeForSource(location);
        pitch /= ((PlayerUtil.getScale(location) - 1) * 0.6F) + 1;

        StepSoundGenerator generator = ((StepSoundSource) location).getStepGenerator(engine).orElse(null);
        if (generator != null) {
            float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
            volume *= generator.getLocalVolume(tickDelta);
            pitch *= generator.getLocalPitch(tickDelta);
        }

        PositionedSoundInstance sound = new UncappedSoundInstance(soundName, volume, pitch, location);

        if (distance > 100) {
            mc.getSoundManager().play(sound, (int) Math.floor(Math.sqrt(distance) / 2));
        } else {
            mc.getSoundManager().play(sound);
        }
    }

    public static class UncappedSoundInstance extends PositionedSoundInstance {
        public UncappedSoundInstance(String soundName, float volume, float pitch, Entity entity) {
            super(getSoundId(soundName, entity),
                    entity.getSoundCategory(),
                    volume, pitch, SoundInstance.createRandom(), false, 0,
                    SoundInstance.AttenuationType.LINEAR,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    false);
        }

        public float getMaxVolume() {
            return 3;
        }

        private static Identifier getSoundId(String name, Entity location) {
            if (name.indexOf(':') >= 0) {
                return Identifier.of(name);
            }

            String domain = "presencefootsteps";

            if (!PlayerUtil.isClientPlayer(location)) {
                domain += "mono"; // Switch to mono if playing another player
            }

            return Identifier.of(domain, name);
        }
    }
}
