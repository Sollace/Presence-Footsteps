package eu.ha3.presencefootsteps.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import eu.ha3.presencefootsteps.sound.State;

/**
 * Solves in-world locations and players into associations. Associations are an
 * extension of Acoustic names, with some special codes. They are derived from
 * the blockmap and defined with these values:<br>
 * <br>
 * The association null is derived from blockmap "NOT_EMITTER" and means the
 * block is NOT MEANT to emit sounds (not equal to "no sound").<br>
 * The association "_NO_ASSOCIATION:xx:yy:zz" is derived from AN ABSENCE of an
 * entry in the blockmap (after solving missing metadata and carpets). xx,yy,zz
 * is the location of the incremented block.<br>
 * Any other association string returned by the findAssociation* methods
 * correspond to an Acoustic name.
 *
 * @author Hurry
 */
public interface Solver {
    String MESSY_FOLIAGE_STRATEGY = "find_messy_foliage";

    /**
     * Play an association.
     */
    boolean playAssociation(LivingEntity ply, Association assos, State eventType);

    /**
     * Find an association for a player particular foot. This will fetch the player
     * angle and use it as a basis to find out what block is below their feet (or
     * which block is likely to be below their feet if the player is walking on the
     * edge of a block when walking over non-emitting blocks like air or water).<br>
     * <br>
     * Returns NOT_EMITTER if no blocks are valid emitting blocks.<br>
     * Returns a string that begins with "_NO_ASSOCIATION" if a matching block was
     * found, but has no association in the blockmap.
     */
    Association findAssociation(LivingEntity ply, double verticalOffsetAsMinus, boolean isRightFoot);

    /**
     * Find an association for a certain block assuming the player is standing on
     * it, using a custom strategy which strategies are defined by the solver.
     */
    Association findAssociation(LivingEntity ply, BlockPos pos, String strategy);
}