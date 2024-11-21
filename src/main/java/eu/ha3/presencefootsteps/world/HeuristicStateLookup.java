package eu.ha3.presencefootsteps.world;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class HeuristicStateLookup {
    private final Function<Block, Optional<Block>> leafBlockCache = Util.memoize(block -> {
        return Stream.of(Registries.BLOCK.getId(block).getPath())
            .flatMap(id -> Arrays.stream(id.split("_")))
            .flatMap(part -> Registries.BLOCK.getOptionalValue(Identifier.of(part + "_leaves")).stream())
            .findFirst();
    });

    @Nullable
    public Block getMostSimilar(Block block) {
        if (block.getDefaultState().getSoundGroup().getStepSound() == SoundEvents.BLOCK_GRASS_STEP) {
            return leafBlockCache.apply(block).orElse(null);
        }
        return null;
    }
}
