package eu.ha3.presencefootsteps.world;

import java.util.HashMap;
import java.util.Map;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.util.ResourceUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.state.BlockState;

public class EntityBlockLookup {
    private static final Identifier BLOCK_MAP = PresenceFootsteps.id("config/blockmap.json");

    private final Lookup<BlockState> globalBlocks = new Lookup<>();
    private final Map<EntityType<?>, Lookup<BlockState>> blocks = new HashMap<>();

    public Lookup<BlockState> getLookup(EntityType<?> sourceType) {
        if (sourceType == EntityTypes.PLAYER) {
            return globalBlocks;
        }
        return blocks.getOrDefault(sourceType, globalBlocks);
    }

    public boolean load(ResourceManager manager) {
        boolean hasConfigurations = globalBlocks.load(ResourceUtils.load(BLOCK_MAP, manager, StateLookup::new));
        blocks.clear();
        blocks.putAll(ResourceUtils.loadDir(FileToIdConverter.json("config/blockmaps/entity"), manager, StateLookup::new, id -> {
            return BuiltInRegistries.ENTITY_TYPE.getOptional(id.withPath(p -> p.replace("config/blockmaps/entity/", "").replace(".json", ""))).orElse(null);
        }, entries -> {
            Lookup<BlockState> lookup = new Lookup<>();
            return lookup.load(entries, globalBlocks) ? lookup : null;
        }));
        return hasConfigurations || !blocks.isEmpty();
    }
}
