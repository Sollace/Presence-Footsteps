package eu.ha3.presencefootsteps.world;

import java.util.function.Function;

import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

public record ContextualBlockState(EntityType<?> type, BlockState state) {
    private static final Function<EntityType<?>, Function<BlockState, ContextualBlockState>> CACHE = Util.memoize(
            entityType -> Util.memoize(
                    state -> new ContextualBlockState(entityType, state)
    ));


    public static ContextualBlockState of(EntityType<?> type, BlockState state) {
        return CACHE.apply(type).apply(state);
    }
}
