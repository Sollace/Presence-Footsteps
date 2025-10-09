package eu.ha3.presencefootsteps.sound.acoustics;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import eu.ha3.presencefootsteps.world.Association;
import eu.ha3.presencefootsteps.world.SoundsKey;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.BlockSoundGroup;

import java.util.Map;

public class AcousticsPlayer implements AcousticLibrary {
    private final Map<String, Acoustic> acoustics = new Object2ObjectOpenHashMap<>();

    private final SoundPlayer soundPlayer;

    public AcousticsPlayer(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    @Override
    public void addAcoustic(String name, Acoustic acoustic) {
        if (acoustics.put(name, acoustic) != null) {
            PresenceFootsteps.logger.info("Duplicate acoustic: " + name);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void playStep(Association association, State event, Options options) {
        if (association.isSilent()) {
            return;
        }

        if (association.dry().isResult()) {
            playAcoustic(association.source(), association.dry(), event, options);
        } else if (!association.state().isLiquid()) {
            BlockSoundGroup soundType = association.state().getSoundGroup();
            BlockState above = association.source().getEntityWorld().getBlockState(association.pos().up());

            if (above.isOf(Blocks.SNOW)) {
                soundType = above.getSoundGroup();
            }

            soundPlayer.playSound(association.source(),
                    soundType.getStepSound().id().toString(),
                    soundType.getVolume() * 0.15F,
                    soundType.getPitch(),
                    options
            );
        }

        if (association.wet().isEmitter() && Options.WET_VOLUME_OPTIONS.get("volume_percentage") > 0.1F) {
            playAcoustic(association.source(), association.wet(), event, options.and(Options.WET_VOLUME_OPTIONS));
        }

        if (association.foliage().isEmitter() && Options.FOLIAGE_VOLUME_OPTIONS.get("volume_percentage") > 0.1F) {
            playAcoustic(association.source(), association.foliage(), event, options.and(Options.FOLIAGE_VOLUME_OPTIONS));
        }
    }

    @Override
    public void playAcoustic(LivingEntity location, SoundsKey sounds, State event, Options inputOptions) {
        for (String acousticName : sounds.names()) {
            Acoustic acoustic = acoustics.get(acousticName);
            if (acoustic == null) {
                PresenceFootsteps.logger.warn("Tried to play a missing acoustic: " + acousticName);
            } else {
                acoustic.playSound(soundPlayer, location, event, inputOptions);
            }
        }
    }

    @Override
    public void think() {
        soundPlayer.think();
    }
}