package eu.ha3.presencefootsteps.world;

import java.util.Optional;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class HeuristicStateLookup {
    private final Function<Block, Optional<Block>> leafBlockCache = Util.memoize(block -> {
        String id = Registries.BLOCK.getId(block).getPath();

        for (String part : id.split("_")) {
            Optional<Block> leavesBlock = Registries.BLOCK.getOrEmpty(new Identifier(part + "_leaves"));
            if (leavesBlock.isPresent()) {
                return leavesBlock;
            }
        }

        return Optional.empty();
    });

    @Nullable
    public Block getMostSimilar(Block block) {
        if (block.getDefaultState().getSoundGroup().getStepSound() == SoundEvents.BLOCK_GRASS_STEP) {
            return leafBlockCache.apply(block).orElse(null);
        }
        return null;
    }
}
