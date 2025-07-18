package eu.ha3.presencefootsteps.world;

import eu.ha3.presencefootsteps.api.DerivedBlock;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Performs a search against all enabled lookups to match a block position, state, and substrate
 * to acoustic association.
 * <p>
 * Priority is given to the golem lookup first.
 * If no steppable entities are found, will check for a matching block state, and if no block state
 * matches appear, will finally use the primitive map to look up a material based on block sound type.
 */
public final class AssociationPool {
    private final LivingEntity entity;
    private final SoundEngine engine;

    private final Solver solver;

    private boolean wasGolem;
    private SoundsKey association;

    public AssociationPool(LivingEntity entity, SoundEngine engine) {
        this.entity = entity;
        this.engine = engine;
        this.solver = engine.getSolver();
    }

    /**
     * Resets the lookup state for a new querying pass.
     */
    public void reset() {
        wasGolem = false;
    }

    /**
     * Returns true if any of the matches in the current pass came from the golem map.
     * <p>
     * Used to bypass wet sounds when walking on boats.
     */
    public boolean wasLastMatchGolem() {
        return wasGolem;
    }

    public Association findAssociation(double verticalOffsetAsMinus, boolean isRightFoot) {
        return solver.findAssociation(this, entity, verticalOffsetAsMinus, isRightFoot);
    }

    public Association findAssociation(BlockPos pos, String strategy) {
        return solver.findAssociation(this, entity, pos, strategy);
    }

    /**
     * Gets matching acoustic names for the supplied position, state, and substrate.
     *
     * @param pos       The block position being queried.
     * @param state     The block state found at the queried position.
     * @param substrate The substrate corresponding to the stage of lookup being performed. One of the values in
     * {@link Substrates}
     * @return The matching acoustic names or {@link Emitter#UNASSIGNED} if no match could be determined.
     */
    public SoundsKey get(BlockPos pos, BlockState state, String substrate) {
        for (Entity golem : entity.getWorld().getOtherEntities(entity, new Box(pos).expand(0.5, 0, 0.5), e -> {
            return !e.isCollidable(entity) || e.getBoundingBox().maxY < entity.getY() + 0.2F;
        })) {
            if ((association = engine.getIsolator().golems().getAssociation(golem.getType(), substrate)).isEmitter()) {
                wasGolem = true;
                return association;
            }
        }

        BlockState baseState = DerivedBlock.getBaseOf(state);

        if (state.isAir()) {
            return SoundsKey.UNASSIGNED;
        }

        if (getForState(state, substrate)
                || (!baseState.isAir() && (
                getForState(baseState, substrate)
                        || (!Substrates.isDefault(substrate) && getForState(baseState, Substrates.DEFAULT))
                        || (getForPrimitive(baseState, substrate))
        ))
                || getForPrimitive(state, substrate)
        ) {
            return association;
        }

        return SoundsKey.UNASSIGNED;
    }

    private boolean getForState(BlockState state, String substrate) {
        return (association =
                engine.getIsolator().blocks(entity.getType()).getAssociation(state, substrate)).isResult();
    }

    private boolean getForPrimitive(BlockState state, String substrate) {
        if (Substrates.isSupplimentary(substrate)) {
            return false;
        }
        BlockSoundGroup sounds = state.getSoundGroup();
        return (association = engine.getIsolator().primitives()
                .getAssociation(sounds.getStepSound(), PrimitiveLookup.getSubstrate(sounds))).isResult();
    }
}