package eu.ha3.presencefootsteps.sound.player;

import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import eu.ha3.presencefootsteps.util.PlayerUtil;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import eu.ha3.presencefootsteps.world.Association;

/**
 * A Library that can also play sounds and default footsteps.
 *
 * @author Hurry
 */
public class ImmediateSoundPlayer implements SoundPlayer, StepSoundPlayer {

    private final Random random = new Random();

    private final DelayedSoundPlayer delayedPlayer = new DelayedSoundPlayer(this);

    private final SoundEngine engine;

    public ImmediateSoundPlayer(SoundEngine engine) {
        this.engine = engine;
    }

    @Override
    public Random getRNG() {
        return random;
    }

    @Override
    public void playStep(Association assos) {
        BlockSoundGroup soundType = assos.getSoundGroup();

        if (!assos.getMaterial().isLiquid() && soundType != null) {
            BlockState beside = assos.getSource().world.getBlockState(assos.getPos().up());

            if (beside.getBlock() == Blocks.SNOW) {
                soundType = Blocks.SNOW.getSoundGroup(beside);
            }

            playAttenuatedSound(assos.getSource(), soundType.getStepSound().getId().toString(), soundType.getVolume() * 0.15F, soundType.getPitch());
        }
    }

    @Override
    public void playSound(LivingEntity location, String soundName, float volume, float pitch, Options options) {

        if (options.containsKey("delay_min") && options.containsKey("delay_max")) {
            delayedPlayer.playSound(location, soundName, volume, pitch, options);

            return;
        }

        playAttenuatedSound(location, soundName, volume, pitch);
    }

    private void playAttenuatedSound(LivingEntity location, String soundName, float volume, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double distance = mc.gameRenderer.getCamera().getPos().squaredDistanceTo(location.getPos());

        volume *= engine.getVolumeForSource(location);
        pitch /= ((PlayerUtil.getScale(location) - 1) * 0.6F) + 1;

        PositionedSoundInstance sound = new UncappedSoundInstance(soundName, volume, pitch, location);

        if (distance > 100) {
            mc.getSoundManager().play(sound, (int) Math.floor(Math.sqrt(distance) / 2));
        } else {
            mc.getSoundManager().play(sound);
        }
    }

    @Override
    public void think() {
        delayedPlayer.think();
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
                return new Identifier(name);
            }

            String domain = "presencefootsteps";

            if (!PlayerUtil.isClientPlayer(location)) {
                domain += "mono"; // Switch to mono if playing another player
            }

            return new Identifier(domain, name);
        }
    }
}
