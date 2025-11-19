package eu.ha3.presencefootsteps.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface ContraptionCollidable {
    BlockState getCollidedStateAt(BlockPos pos);
}
