package eu.ha3.presencefootsteps.sound.player;

import java.util.Random;

import eu.ha3.presencefootsteps.util.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import eu.ha3.presencefootsteps.PresenceFootsteps;
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

        Minecraft mc = Minecraft.getInstance();
        double distance = mc.gameRenderer.mainCamera().position().distanceToSqr(location.position());

        volume *= engine.getVolumeForSource(location);
        pitch /= ((PlayerUtil.getScale(location) - 1) * 0.6F) + 1;

        StepSoundGenerator generator = ((StepSoundSource) location).getStepGenerator(engine).orElse(null);
        if (generator != null) {
            float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            volume *= generator.getLocalVolume(tickDelta);
            pitch *= generator.getLocalPitch(tickDelta);
        }

        SoundManager sm = mc.getSoundManager();
        Identifier soundid = PresenceFootsteps.withDefaultNamespace(soundName);
        if (!PlayerUtil.isClientPlayer(location)) {
            soundid = UncappedSoundInstance.getMonoSoundId(soundid, sm);
        }
        SimpleSoundInstance sound = new UncappedSoundInstance(soundid, volume, pitch, location);

        if (distance > 100) {
            sm.playDelayed(sound, (int) Math.floor(Math.sqrt(distance) / 2));
        } else {
            sm.play(sound);
        }
    }

    public static class UncappedSoundInstance extends SimpleSoundInstance {
        public UncappedSoundInstance(Identifier soundId, float volume, float pitch, Entity entity) {
            super(soundId,
                    entity.getSoundSource(),
                    volume, pitch, SoundInstance.createUnseededRandom(), false, 0,
                    SoundInstance.Attenuation.LINEAR,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    false);
        }

        public float getMaxVolume() {
            return 3;
        }

        public static Identifier getMonoSoundId(Identifier id, SoundManager sm) {
            Identifier monoId = Identifier.fromNamespaceAndPath(id.getNamespace() + "mono", id.getPath());
            // Switch to mono if playing another player and a mono sound is available
            return sm.getSoundEvent(monoId) == null ? id : monoId;
        }
    }
}
