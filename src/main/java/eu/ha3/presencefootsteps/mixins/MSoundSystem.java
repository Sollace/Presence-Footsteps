package eu.ha3.presencefootsteps.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import eu.ha3.presencefootsteps.sound.player.ImmediateSoundPlayer;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.util.Mth;

@Mixin(SoundEngine.class)
abstract class MSoundSystem {
    @Shadow
    private @Final Options options;

    @ModifyReturnValue(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"))
    private float adjustSoundVolume(float volume, SoundInstance sound) {
        if (sound instanceof ImmediateSoundPlayer.UncappedSoundInstance t) {
            return Mth.clamp(sound.getVolume() * options.getFinalSoundSourceVolume(sound.getSource()), 0, t.getMaxVolume());
        }
        return volume;
    }
}
