package eu.ha3.presencefootsteps.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface DerivedBlock {
    static BlockState getBaseOf(BlockState state) {
        return ((DerivedBlock)state.getBlock()).getBaseBlockState();
    }

    BlockState getBaseBlockState();

    interface Settings {
        @Nullable
        Block getBaseBlock();

        void setBaseBlock(Block block);
    }
}
