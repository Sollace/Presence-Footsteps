package eu.ha3.presencefootsteps.mixins;

import net.minecraft.client.option.GameOptions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import eu.ha3.presencefootsteps.sound.player.ImmediateSoundPlayer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.math.MathHelper;

@Mixin(SoundSystem.class)
abstract class MSoundSystem {
    @Shadow
    @Final
    private GameOptions options;

    @Inject(method = "getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F", at = @At("HEAD"), cancellable = true)
    private void onGetAdjustedVolume(SoundInstance sound, CallbackInfoReturnable<Float> info) {
        if (sound instanceof ImmediateSoundPlayer.UncappedSoundInstance t) {
            info.setReturnValue(MathHelper.clamp(t.getVolume() * options.getSoundVolume(t.getCategory()), 0, t.getMaxVolume()));
        }
    }
}
