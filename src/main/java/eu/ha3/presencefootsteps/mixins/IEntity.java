package eu.ha3.presencefootsteps.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import eu.ha3.presencefootsteps.sound.StepSoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(Entity.class)
abstract class IEntity {
    @Inject(method = "walkingStepSound", at = @At("HEAD"), cancellable = true)
    private void onPlayStepSounds(BlockPos pos, BlockState state, CallbackInfo info) {
        if (this instanceof StepSoundSource s && s.isStepBlocked() && ((Entity)(Object)this).level().isClientSide()) {
            info.cancel();
        }
    }
}
