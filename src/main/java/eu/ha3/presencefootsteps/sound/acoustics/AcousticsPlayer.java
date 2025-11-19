package eu.ha3.presencefootsteps.sound.acoustics;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import eu.ha3.presencefootsteps.world.Association;
import eu.ha3.presencefootsteps.world.SoundsKey;

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

    @Override
    public void playStep(Association association, State event, Options options) {
        if (association.isSilent()) {
            return;
        }

        if (association.dry().isResult() && !association.dry().isVanilla()) {
            playAcoustic(association.source(), association.dry(), event, options);
        } else {
            playVanillaStep(association, options);
        }

        if (association.wet().isEmitter() && Options.WET_VOLUME_OPTIONS.get("volume_percentage") > 0.1F) {
            playAcoustic(association.source(), association.wet(), event, options.and(Options.WET_VOLUME_OPTIONS));
        }

        if (association.foliage().isEmitter() && Options.FOLIAGE_VOLUME_OPTIONS.get("volume_percentage") > 0.1F) {
            playAcoustic(association.source(), association.foliage(), event, options.and(Options.FOLIAGE_VOLUME_OPTIONS));
        }
    }

    @SuppressWarnings("deprecation")
    private void playVanillaStep(Association association, Options options) {
        if (association.state().liquid()) {
            return;
        }
        SoundType soundType = association.state().getSoundType();
        BlockState above = association.source().level().getBlockState(association.pos().above());

        if (above.is(Blocks.SNOW)) {
            soundType = above.getSoundType();
        }

        soundPlayer.playSound(association.source(),
                soundType.getStepSound().location().toString(),
                soundType.getVolume() * 0.15F,
                soundType.getPitch(),
                options
        );
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