package eu.ha3.presencefootsteps.world;

import eu.ha3.presencefootsteps.compat.ContraptionCollidable;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import eu.ha3.presencefootsteps.util.PlayerUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PFSolver implements Solver {
    private static final double TRAP_DOOR_OFFSET = 0.1;

    private final SoundEngine engine;

    private long lastUpdateTime;
    private final Long2ObjectOpenHashMap<Association> associationCache = new Long2ObjectOpenHashMap<>();

    public PFSolver(SoundEngine engine) {
        this.engine = engine;
    }

    private BlockState getBlockStateAt(Entity entity, BlockPos pos) {
        Level world = entity.level();
        BlockState state = world.getBlockState(pos);

        if (state.isAir() && (entity instanceof ContraptionCollidable collidable)) {
            state = collidable.getCollidedStateAt(pos);
        }

        return state.getAppearance(world, pos, Direction.UP, state, pos);
    }

    private AABB getCollider(Entity player) {
        AABB collider = player.getBoundingBox();
        // normalize to the bottom of the block
        // so we can detect carpets on top of fences
        collider = collider.move(0, -(collider.minY - Math.floor(collider.minY)), 0);

        double expansionRatio = 0.1;

        // add buffer
        collider = collider.inflate(expansionRatio);
        if (player.isSprinting()) {
            collider = collider.inflate(0.3, 0.5, 0.3);
        }
        return collider;
    }

    private boolean checkCollision(Level world, BlockState state, BlockPos pos, AABB collider) {
        VoxelShape shape = state.getCollisionShape(world, pos);
        if (shape.isEmpty()) {
            shape = state.getCollisionShape(world, pos);
        }
        return shape.isEmpty() || shape.bounds().move(pos).intersects(collider);
    }

    @Override
    public Association findAssociation(AssociationPool associations, LivingEntity ply, BlockPos pos, String strategy) {
        if (!MESSY_FOLIAGE_STRATEGY.equals(strategy)) {
            return Association.NOT_EMITTER;
        }

        pos = pos.above();
        BlockState above = getBlockStateAt(ply, pos);

        Lookup<BlockState> lookup = engine.getIsolator().blocks(ply.getType());
        SoundsKey foliage = lookup.getAssociation(above, Substrates.FOLIAGE);

        // we discard the normal block association, and mark the foliage as detected
        if (foliage.isEmitter() && lookup.getAssociation(above, Substrates.MESSY) == SoundsKey.MESSY_GROUND) {
            return Association.of(above, pos, ply, false, SoundsKey.NON_EMITTER, SoundsKey.NON_EMITTER, foliage);
        }

        return Association.NOT_EMITTER;
    }

    @Override
    public Association findAssociation(AssociationPool associations, LivingEntity ply, double verticalOffsetAsMinus, boolean isRightFoot) {

        double rot = Math.toRadians(Mth.wrapDegrees(ply.getYRot()));

        Vec3 pos = ply.position();

        float feetDistanceToCenter = 0.2f * (isRightFoot ? -1 : 1)
                * PlayerUtil.getScale(ply) // scale foot offset by the player's scale
        ;

        BlockPos footPos = BlockPos.containing(
            pos.x + Math.cos(rot) * feetDistanceToCenter,
            ply.getBoundingBox().min(Axis.Y) - TRAP_DOOR_OFFSET - verticalOffsetAsMinus,
            pos.z + Math.sin(rot) * feetDistanceToCenter
        );

        if (!(ply instanceof RemotePlayer)) {
            Vec3 vel = ply.getDeltaMovement();

            if (vel.lengthSqr() != 0 && Math.abs(vel.y) < 0.004) {
                return Association.NOT_EMITTER; // Don't play sounds on every tiny bounce
            }
        }

        long time = ply.level().getGameTime();
        if (time != lastUpdateTime) {
            lastUpdateTime = time;
            associationCache.clear();
        }

        Association cached = associationCache.get(footPos.asLong());
        if (cached != null) {
            return cached;
        }

        AABB collider = getCollider(ply);

        BlockPos.MutableBlockPos mutableFootPos = footPos.mutable();

        if (feetDistanceToCenter > 1) {
            for (BlockPos underfootPos : BlockPos.withinManhattan(footPos, (int)feetDistanceToCenter, 2, (int)feetDistanceToCenter)) {
                mutableFootPos.set(underfootPos);
                Association assos = findAssociation(associations, ply, collider, underfootPos, mutableFootPos);
                if (assos.isResult()) {
                    associationCache.put(footPos.asLong(), assos);
                    return assos;
                }
            }
        }

        Association assos = findAssociation(associations, ply, collider, footPos, mutableFootPos);
        associationCache.put(footPos.asLong(), assos);
        return assos;
    }

    @SuppressWarnings("deprecation")
    private Association findAssociation(AssociationPool associations, LivingEntity player, AABB collider, BlockPos originalFootPos, BlockPos.MutableBlockPos pos) {
        Association association;

        if (engine.getConfig().isVisualiserRunning()) {
            for (int i = 0; i < 10; i++) {
                player.level().addAlwaysVisibleParticle(ParticleTypes.DOLPHIN,
                    pos.getX() + 0.5,
                    pos.getY() + 1,
                    pos.getZ() + 0.5, 0, 0, 0);
            }
        }

        if ((association = findAssociation(associations, player, pos, collider)).isResult()) {
            if (!association.state().liquid()) {
                if (engine.getConfig().isVisualiserRunning()) {
                    player.level().addAlwaysVisibleParticle(ParticleTypes.DUST_PLUME,
                            association.pos().getX() + 0.5,
                            association.pos().getY() + 0.9,
                            association.pos().getZ() + 0.5, 0, 0, 0);
                }
                return association;
            }
        }

        double radius = 0.4;
        int[] xValues = new int[] {
                Mth.floor(collider.min(Axis.X) - radius),
                pos.getX(),
                Mth.floor(collider.max(Axis.X) + radius)
        };
        int[] zValues = new int[] {
                Mth.floor(collider.min(Axis.Z) - radius),
                pos.getZ(),
                Mth.floor(collider.max(Axis.Z) + radius)
        };

        for (int x : xValues) {
            for (int z : zValues) {
                if (x != originalFootPos.getX() || z != originalFootPos.getZ()) {
                    pos.set(x, originalFootPos.getY(), z);
                    if (engine.getConfig().isVisualiserRunning()) {
                        for (int i = 0; i < 10; i++) {
                            player.level().addAlwaysVisibleParticle(ParticleTypes.DOLPHIN,
                                pos.getX() + 0.5,
                                pos.getY() + 1,
                                pos.getZ() + 0.5, 0, 0, 0);
                        }
                    }
                    if ((association = findAssociation(associations, player, pos, collider)).isResult()) {
                        if (!association.state().liquid()) {
                            if (engine.getConfig().isVisualiserRunning()) {
                                player.level().addAlwaysVisibleParticle(ParticleTypes.DUST_PLUME,
                                        association.pos().getX() + 0.5,
                                        association.pos().getY() + 0.9,
                                        association.pos().getZ() + 0.5, 0, 0, 0);
                            }
                            return association;
                        }
                    }
                }
            }
        }
        pos.set(originalFootPos);

        BlockState state = getBlockStateAt(player, pos);

        if (state.liquid()) {
            if (state.getFluidState().is(FluidTags.LAVA)) {
                return Association.of(state, pos.below(), player, false, SoundsKey.LAVAFINE, SoundsKey.NON_EMITTER, SoundsKey.NON_EMITTER);
            }
            return Association.of(state, pos.below(), player, false, SoundsKey.WATERFINE, SoundsKey.NON_EMITTER, SoundsKey.NON_EMITTER);
        }

        return association;
    }

    private Association findAssociation(AssociationPool associations, LivingEntity entity, BlockPos.MutableBlockPos pos, AABB collider) {
        associations.reset();
        BlockState target = getBlockStateAt(entity, pos);

        // Try to see if the block above is a carpet...
        pos.move(Direction.UP);
        final boolean hasRain = entity.level().isRainingAt(pos);
        BlockState carpet = getBlockStateAt(entity, pos);
        VoxelShape shape = carpet.getCollisionShape(entity.level(), pos);
        boolean isValidCarpet = !shape.isEmpty() && shape.max(Axis.Y) < 0.3F;
        SoundsKey association = SoundsKey.UNASSIGNED;
        SoundsKey foliage = SoundsKey.UNASSIGNED;
        SoundsKey wetAssociation = SoundsKey.UNASSIGNED;

        if (isValidCarpet && (association = associations.get(pos, carpet, Substrates.CARPET)).isEmitter() && !association.isSilent()) {
            target = carpet;
            // reference frame moved up by 1
        } else {
            // This condition implies that if the carpet is NOT_EMITTER, solving will
            // CONTINUE with the actual block surface the player is walking on
            pos.move(Direction.DOWN);
            association = associations.get(pos, target, Substrates.DEFAULT);

            // If the block surface we're on is not an emitter, check for fences below us
            if (!association.isEmitter() || !association.isResult()) {
                pos.move(Direction.DOWN);
                BlockState fence = getBlockStateAt(entity, pos);

                // Only check fences if we're actually touching them
                if (checkCollision(entity.level(), fence, pos, collider) && (association = associations.get(pos, fence, Substrates.FENCE)).isResult()) {
                    carpet = target;
                    target = fence;
                    // reference frame moved down by 1
                } else {
                    pos.move(Direction.UP);
                }
            }

            if (engine.getConfig().foliageSoundsVolume.get() > 0) {
                if (entity.getItemBySlot(EquipmentSlot.FEET).isEmpty() || entity.isSprinting()) {
                    if (association.isEmitter() && carpet.getCollisionShape(entity.level(), pos).isEmpty()) {
                        // This condition implies that foliage over a NOT_EMITTER block CANNOT PLAY
                        // This block must not be executed if the association is a carpet
                        pos.move(Direction.UP);
                        foliage = associations.get(pos, carpet, Substrates.FOLIAGE);
                        pos.move(Direction.DOWN);
                    }
                }
            }
        }

        // Check collision against small blocks
        if (association.isResult() && !checkCollision(entity.level(), target, pos, collider)) {
            association = SoundsKey.UNASSIGNED;
        }

        if (association.isEmitter() && (hasRain
                || (!associations.wasLastMatchGolem() && (
                   (target.getFluidState().is(FluidTags.WATER) && !target.isFaceSturdy(entity.level(), pos, Direction.UP))
                || (carpet.getFluidState().is(FluidTags.WATER) && !carpet.isFaceSturdy(entity.level(), pos, Direction.UP))
        )))) {
            // Only if the block is open to the sky during rain
            // or the block is submerged
            // or the block is waterlogged
            // then append the wet effect to footsteps
            wetAssociation = associations.get(pos, target, Substrates.WET);
        }

        return Association.of(target, pos, entity, associations.wasLastMatchGolem() && entity.onGround(), association, wetAssociation, foliage);
    }
}
