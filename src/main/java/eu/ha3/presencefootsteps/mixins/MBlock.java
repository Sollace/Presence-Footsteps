package eu.ha3.presencefootsteps.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.api.DerivedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;


@Mixin(Block.class)
abstract class MAbstractBlock extends BlockBehaviour implements DerivedBlock {
    MAbstractBlock() { super(null); }

    @Override
    public BlockState getBaseBlockState() {
        Block baseBlock = ((DerivedBlock.Settings)properties).getBaseBlock();
        if (baseBlock == null) {
            baseBlock = PresenceFootsteps.getInstance().getEngine().getIsolator().heuristics().getMostSimilar((Block)(Object)this);
        }
        return (baseBlock == null ? Blocks.AIR : baseBlock).defaultBlockState();
    }
}

@Mixin(StairBlock.class)
abstract class MStairsBlock implements DerivedBlock {
    @Accessor("baseState")
    @Override
    public abstract BlockState getBaseBlockState();
}

@Mixin(BlockBehaviour.Properties.class)
abstract class MBlockSettings implements DerivedBlock.Settings {
    @Nullable
    private Block baseBlock;

    @Override
    public void setBaseBlock(Block baseBlock) {
        this.baseBlock = baseBlock;
    }

    @Override
    @Nullable
    public Block getBaseBlock() {
        return baseBlock;
    }

    @Inject(method = "ofLegacyCopy", at = @At("RETURN"))
    private static void onCopy(BlockBehaviour block, CallbackInfoReturnable<BlockBehaviour.Properties> info) {
        if (block instanceof Block b) {
            ((DerivedBlock.Settings)info.getReturnValue()).setBaseBlock(b);
        }
    }
}
