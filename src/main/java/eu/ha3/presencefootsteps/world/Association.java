package eu.ha3.presencefootsteps.world;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;


public record Association (
        BlockState state,
        BlockPos pos,
        @Nullable LivingEntity source,
        boolean forcePlay,

        SoundsKey dry,
        SoundsKey wet,
        SoundsKey foliage
) {
    public static final Association NOT_EMITTER = new Association(Blocks.AIR.defaultBlockState(), BlockPos.ZERO, null, false, SoundsKey.NON_EMITTER, SoundsKey.NON_EMITTER, SoundsKey.NON_EMITTER);

    public static Association of(BlockState state, BlockPos pos, LivingEntity source, boolean forcePlay, SoundsKey dry, SoundsKey wet, SoundsKey foliage) {
        if (dry.isSilent() && wet.isSilent() && foliage.isSilent()) {
            return NOT_EMITTER;
        }
        return new Association(state, pos.immutable(), source, forcePlay, dry, wet, foliage);
    }

    public boolean isResult() {
        return dry.isResult() || wet.isResult() || foliage.isResult();
    }

    public boolean isSilent() {
        return this == NOT_EMITTER || (state.isAir() && !forcePlay);
    }

    public boolean dataEquals(Association other) {
        return Objects.equals(dry, other.dry);
    }
}
