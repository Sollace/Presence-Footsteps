package eu.ha3.presencefootsteps.world;

import eu.ha3.presencefootsteps.compat.ContraptionCollidable;
import eu.ha3.presencefootsteps.sound.Isolator;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.util.PlayerUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;

public class PFSolver implements Solver {
    private static final Logger logger = LogManager.getLogger("PFSolver");

    private static final double TRAP_DOOR_OFFSET = 0.1;

    private final Isolator isolator;

    public PFSolver(Isolator isolator) {
        this.isolator = isolator;
    }

    @Override
    public boolean playAssociation(LivingEntity ply, Association assos, State eventType) {
        if (!assos.isResult()) {
            return false;
        }

        if (!assos.isNotEmitter()) {
            assos = assos.at(ply);

            if (assos.hasAssociation()) {
                isolator.getAcoustics().playAcoustic(assos, eventType, Options.EMPTY);
            } else {
                isolator.getStepPlayer().playStep(assos);
            }
        }

        return true;
    }

    @Override
    public Association findAssociation(LivingEntity ply, double verticalOffsetAsMinus, boolean isRightFoot) {

        double rot = Math.toRadians(MathHelper.wrapDegrees(ply.getYaw()));

        Vec3d pos = ply.getPos();

        float feetDistanceToCenter = 0.2f * (isRightFoot ? -1 : 1)
                * PlayerUtil.getScale(ply) // scale foot offset by the player's scale
        ;

        BlockPos footPos = BlockPos.ofFloored(
            pos.x + Math.cos(rot) * feetDistanceToCenter,
            ply.getBoundingBox().getMin(Axis.Y) - TRAP_DOOR_OFFSET - verticalOffsetAsMinus,
            pos.z + Math.sin(rot) * feetDistanceToCenter
        );

        if (feetDistanceToCenter > 1) {
            for (BlockPos underfootPos : BlockPos.iterateOutwards(footPos, (int)feetDistanceToCenter, 2, (int)feetDistanceToCenter)) {
                Association assos = findAssociation(ply, underfootPos);
                if (assos.hasAssociation()) {
                    return assos;
                }
            }
        }

        return findAssociation(ply, footPos);
    }

    private Association findAssociation(Entity player, BlockPos pos) {

        if (!(player instanceof OtherClientPlayerEntity)) {
            Vec3d vel = player.getVelocity();

            if (vel.lengthSquared() != 0 && Math.abs(vel.y) < 0.004) {
                return Association.NOT_EMITTER; // Don't play sounds on every tiny bounce
            }
        }

        Box collider = player.getBoundingBox();
        // normalize to the bottom of the block
        // so we can detect carpets on top of fences
        collider = collider.offset(0, -(collider.minY - Math.floor(collider.minY)), 0);

        double expansionRatio = 0.1;

        // add buffer
        collider = collider.expand(expansionRatio);
        if (player.isSprinting()) {
            collider = collider.expand(0.3, 0.5, 0.3);
        }

        Association worked = findAssociation(player, pos, collider);

        // If it didn't work, the player has walked over the air on the border of a block.
        // ------ ------ --> z
        // | o | < player is here
        // wool | air |
        // ------ ------
        // |
        // V z
        if (!worked.isNull()) {
            return worked;
        }

        // Create a trigo. mark contained inside the block the player is over
        double xdang = (player.getX() - pos.getX()) * 2 - 1;
        double zdang = (player.getZ() - pos.getZ()) * 2 - 1;
        // -1 0 1
        // ------- -1
        // | o |
        // | + | 0 --> x
        // | |
        // ------- 1
        // |
        // V z

        // If the player is at the edge of that
        if (Math.max(Math.abs(xdang), Math.abs(zdang)) <= 0.2f) {
            return worked;
        }
        // Find the maximum absolute value of X or Z
        boolean isXdangMax = Math.abs(xdang) > Math.abs(zdang);
        // --------------------- ^ maxofZ-
        // | . . |
        // | . . |
        // | o . . |
        // | . . |
        // | . |
        // < maxofX- maxofX+ >
        // Take the maximum border to produce the sound
        if (isXdangMax) { // If we are in the positive border, add 1, else subtract 1
            worked = findAssociation(player, pos.east(xdang > 0 ? 1 : -1), collider);
        } else {
            worked = findAssociation(player, pos.south(zdang > 0 ? 1 : -1), collider);
        }

        // If that didn't work, then maybe the footstep hit in the
        // direction of walking
        // Try with the other closest block
        if (!worked.isNull()) {
            return worked;
        }

        // Take the maximum direction and try with the orthogonal direction of it
        if (isXdangMax) {
            return findAssociation(player, pos.south(zdang > 0 ? 1 : -1), collider);
        }

        return findAssociation(player, pos.east(xdang > 0 ? 1 : -1), collider);
    }

    private String findForGolem(World world, BlockPos pos, String substrate) {
        List<Entity> golems = world.getEntitiesByClass(Entity.class, new Box(pos).expand(0.5, 0, 0.5), e -> !(e instanceof PlayerEntity));

        if (!golems.isEmpty()) {
            String golem = isolator.getGolemMap().getAssociation(golems.get(0).getType(), substrate);

            if (Emitter.isEmitter(golem)) {
                logger.debug("Golem detected: " + golem);

                return golem;
            }
        }

        return Emitter.UNASSIGNED;
    }

    private BlockState getBlockStateAt(Entity entity, BlockPos pos) {
        World world = entity.getWorld();
        BlockState state = world.getBlockState(pos);

        if (state.isAir() && (entity instanceof ContraptionCollidable collidable)) {
            state = collidable.getCollidedStateAt(pos);
        }
        return state;
    }

    private Association findAssociation(Entity entity, BlockPos pos, Box collider) {
        BlockState in = getBlockStateAt(entity, pos);

        BlockPos up = pos.up();
        BlockState above = getBlockStateAt(entity, up);
        // Try to see if the block above is a carpet...

        String association = findForGolem(entity.getWorld(), up, Lookup.CARPET_SUBSTRATE);
        boolean wasGolem = false;
        String wetAssociation = Emitter.NOT_EMITTER;

        if (!Emitter.isEmitter(association)) {
            association = isolator.getBlockMap().getAssociation(above, Lookup.CARPET_SUBSTRATE);
        } else {
            wasGolem = true;
        }

        if (Emitter.isEmitter(association)) {
            logger.debug("Carpet detected: " + association);
            pos = up;
            in = above;
        } else {
            // This condition implies that if the carpet is NOT_EMITTER, solving will
            // CONTINUE with the actual block surface the player is walking on
                              // check the height of the block. If it's something very short, like a carpet, also look through it
            if (in.isAir() || in.getCollisionShape(entity.getWorld(), pos).getMax(Axis.Y) < 0.3F) {
                BlockPos down = pos.down();
                BlockState below = getBlockStateAt(entity, down);

                association = isolator.getBlockMap().getAssociation(below, Lookup.FENCE_SUBSTRATE);

                if (Emitter.isResult(association)) {
                    logger.debug("Fence detected: " + association);
                    pos = down;
                    in = below;
                }
            }

            VoxelShape shape = in.getCollisionShape(entity.getWorld(), pos);
            if (shape.isEmpty()) {
                shape = in.getOutlineShape(entity.getWorld(), pos);
            }
            if (!shape.isEmpty() && !shape.getBoundingBox().offset(pos).intersects(collider)) {
                logger.debug("Skipping due to hitbox miss");
                return Association.NOT_EMITTER;
            }

            if (!Emitter.isResult(association)) {
                association = findForGolem(entity.getWorld(), pos, Lookup.EMPTY_SUBSTRATE);

                if (!Emitter.isEmitter(association)) {
                    association = isolator.getBlockMap().getAssociation(in, Lookup.EMPTY_SUBSTRATE);
                } else {
                    wasGolem = true;
                }
            }

            if (Emitter.isEmitter(association)) {
                // This condition implies that foliage over a NOT_EMITTER block CANNOT PLAY
                // This block most not be executed if the association is a carpet
                String foliage = isolator.getBlockMap().getAssociation(above, Lookup.FOLIAGE_SUBSTRATE);

                if (Emitter.isEmitter(foliage)) {
                    logger.debug("Foliage detected: " + foliage);
                    association += "," + foliage;
                }
            }
        }

        if (Emitter.isEmitter(association) && (entity.getWorld().hasRain(up) || (!wasGolem && (in.getFluidState().isIn(FluidTags.WATER) || above.getFluidState().isIn(FluidTags.WATER))))) {
            // Only if the block is open to the sky during rain
            // or the block is submerged
            // or the block is waterlogged
            // then append the wet effect to footsteps
            String wet = isolator.getBlockMap().getAssociation(in, Lookup.WET_SUBSTRATE);

            if (Emitter.isEmitter(wet)) {
                logger.debug("Wet block detected: " + wet);
                wetAssociation = wet;
            }
        }

        // Player has stepped on a non-emitter block as defined in the blockmap
        if (Emitter.isNonEmitter(association) && Emitter.isNonEmitter(wetAssociation)) {
            return Association.NOT_EMITTER;
        }

        if (Emitter.isResult(association)) {
            return new Association(in, pos).withDry(association).withWet(wetAssociation);
        }

        if (in.isAir()) {
            return Association.NOT_EMITTER;
        }

        // Check for primitive in register
        BlockSoundGroup sounds = in.getSoundGroup();
        String substrate = String.format(Locale.ENGLISH, "%.2f_%.2f", sounds.volume, sounds.pitch);
        String primitive = isolator.getPrimitiveMap().getAssociation(sounds, substrate);

        if (Emitter.isResult(primitive)) {
            return new Association(in, pos).withDry(primitive).withWet(wetAssociation);
        }

        return Association.NOT_EMITTER;
    }

    @Override
    public Association findAssociation(LivingEntity ply, BlockPos pos, String strategy) {
        if (!MESSY_FOLIAGE_STRATEGY.equals(strategy)) {
            return Association.NOT_EMITTER;
        }

        BlockState above = getBlockStateAt(ply, pos.up());

        String foliage = isolator.getBlockMap().getAssociation(above, Lookup.FOLIAGE_SUBSTRATE);

        if (!Emitter.isEmitter(foliage)) {
            return Association.NOT_EMITTER;
        }

        // we discard the normal block association, and mark the foliage as detected
        if (Emitter.MESSY_GROUND.equals(isolator.getBlockMap().getAssociation(above, Lookup.MESSY_SUBSTRATE))) {
            return new Association(above, pos.up()).withDry(foliage);
        }

        return Association.NOT_EMITTER;
    }
}
