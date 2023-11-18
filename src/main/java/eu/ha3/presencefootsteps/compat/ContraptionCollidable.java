package eu.ha3.presencefootsteps.compat;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public interface ContraptionCollidable {
    BlockState getCollidedStateAt(BlockPos pos);
}
