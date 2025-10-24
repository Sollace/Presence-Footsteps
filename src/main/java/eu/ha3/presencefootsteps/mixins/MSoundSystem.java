package eu.ha3.presencefootsteps.mixins;

import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import eu.ha3.presencefootsteps.sound.player.ImmediateSoundPlayer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.math.MathHelper;

@Mixin(SoundSystem.class)
abstract class MSoundSystem {
    @Shadow
    private @Final GameOptions options;

    @ModifyReturnValue(method = "getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F", at = @At("RETURN"))
    private float adjustSoundVolume(float volume, SoundInstance sound) {
        if (sound instanceof ImmediateSoundPlayer.UncappedSoundInstance t) {
            return MathHelper.clamp(sound.getVolume() * options.getSoundVolume(sound.getCategory()), 0, t.getMaxVolume());
        }
        return volume;
    }
}
