package eu.ha3.presencefootsteps.world;

import java.util.function.Function;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Util;

public record ContextualBlockState(EntityType<?> type, BlockState state) {
    private static final Function<EntityType<?>, Function<BlockState, ContextualBlockState>> CACHE = Util.memoize(
            entityType -> Util.memoize(
                    state -> new ContextualBlockState(entityType, state)
    ));


    public static ContextualBlockState of(EntityType<?> type, BlockState state) {
        return CACHE.apply(type).apply(state);
    }
}
