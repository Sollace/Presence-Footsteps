package eu.ha3.presencefootsteps.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import eu.ha3.presencefootsteps.sound.StepSoundSource;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

@Mixin(Entity.class)
abstract class IEntity {
    @Inject(method = "playStepSounds", at = @At("HEAD"), cancellable = true)
    private void playStepSounds(BlockPos pos, BlockState state, CallbackInfo info) {
        if (this instanceof StepSoundSource s && s.isStepBlocked() && ((Entity)(Object)this).getEntityWorld().isClient()) {
            info.cancel();
        }
    }
}
