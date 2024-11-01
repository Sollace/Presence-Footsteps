package eu.ha3.presencefootsteps.mixins.compat.create;

import org.apache.logging.log4j.util.TriConsumer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import eu.ha3.presencefootsteps.compat.ContraptionCollidable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Mixin(value = Entity.class, priority = 9999 /* Run us last */)
abstract class MEntity implements ContraptionCollidable {

    private int presenceFootsteps$lastCollidedContraptionStateTick = -1;
    private BlockState presenceFootsteps$lastCollidedContraptionState = Blocks.AIR.getDefaultState();

    @Dynamic(
        value = "create$forCollision(center, consumer) - Private member injected by Create. See: https://github.com/Fabricators-of-Create/Create/blob/49cc17e3de33c965b1c409130abe436821f7410c/src/main/java/com/simibubi/create/foundation/mixin/client/EntityContraptionInteractionMixin.java#L81C21-L81C21"
    )
    @Shadow(aliases = {
            "forCollision", "create$forCollision"
    })
    private void create$forCollision(Vec3d anchorPos, TriConsumer<Object, BlockState, BlockPos> action) {}

    @Override
    public BlockState getCollidedStateAt(BlockPos pos) {
        if (presenceFootsteps$lastCollidedContraptionStateTick != ((Entity)(Object)this).age) {
            presenceFootsteps$lastCollidedContraptionStateTick = ((Entity)(Object)this).age;
            create$forCollision(((Entity)(Object)this).getPos().add(0, -0.2, 0), (unused, state, p) -> {
                if (pos.equals(p)) {
                    presenceFootsteps$lastCollidedContraptionState = state;
                }
            });
        }
        return presenceFootsteps$lastCollidedContraptionState;
    }
}
