package eu.ha3.presencefootsteps.sound;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.config.Variator;
import eu.ha3.presencefootsteps.sound.acoustics.Acoustic;
import eu.ha3.presencefootsteps.sound.acoustics.AcousticLibrary;
import eu.ha3.presencefootsteps.sound.acoustics.AcousticsPlayer;
import eu.ha3.presencefootsteps.sound.generator.Locomotion;
import eu.ha3.presencefootsteps.sound.player.DelayedSoundPlayer;
import eu.ha3.presencefootsteps.util.JsonObjectWriter;
import eu.ha3.presencefootsteps.util.ResourceUtils;
import eu.ha3.presencefootsteps.util.BlockReport.Reportable;
import eu.ha3.presencefootsteps.world.BiomeVarianceLookup;
import eu.ha3.presencefootsteps.world.GolemLookup;
import eu.ha3.presencefootsteps.world.HeuristicStateLookup;
import eu.ha3.presencefootsteps.world.Index;
import eu.ha3.presencefootsteps.world.LocomotionLookup;
import eu.ha3.presencefootsteps.world.Lookup;
import eu.ha3.presencefootsteps.world.PrimitiveLookup;
import eu.ha3.presencefootsteps.world.StateLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public record Isolator (
        Variator variator,
        Index<Entity, Locomotion> locomotions,
        HeuristicStateLookup heuristics,
        Lookup<EntityType<?>> golems,
        Lookup<BlockState> globalBlocks,
        Map<EntityType<?>, Lookup<BlockState>> blocks,
        Index<Identifier, BiomeVarianceLookup.BiomeVariance> biomes,
        Lookup<SoundEvent> primitives,
        AcousticLibrary acoustics
    ) implements Reportable {
    private static final Identifier BLOCK_MAP = PresenceFootsteps.id("config/blockmap.json");
    private static final Identifier BIOME_MAP = PresenceFootsteps.id("config/biomevariancemap.json");
    private static final Identifier GOLEM_MAP = PresenceFootsteps.id("config/golemmap.json");
    private static final Identifier LOCOMOTION_MAP = PresenceFootsteps.id("config/locomotionmap.json");
    private static final Identifier PRIMITIVE_MAP = PresenceFootsteps.id("config/primitivemap.json");
    private static final Identifier ACOUSTICS = PresenceFootsteps.id("config/acoustics");
    private static final Identifier VARIATOR = PresenceFootsteps.id("config/variator.json");

    public Isolator(SoundEngine engine) {
        this(new Variator(),
                new LocomotionLookup(engine.getConfig()),
                new HeuristicStateLookup(),
                new Lookup<>(),
                new Lookup<>(),
                new HashMap<>(),
                new BiomeVarianceLookup(),
                new Lookup<>(),
                new AcousticsPlayer(new DelayedSoundPlayer(engine.soundPlayer))
        );
    }

    public Lookup<BlockState> blocks(EntityType<?> sourceType) {
        if (sourceType == EntityType.PLAYER) {
            return globalBlocks();
        }
        return blocks.getOrDefault(sourceType, globalBlocks());
    }

    public boolean load(ResourceManager manager) {
        boolean hasConfigurations = false;
        hasConfigurations |= globalBlocks().load(ResourceUtils.load(BLOCK_MAP, manager, StateLookup::new));

        blocks.clear();
        blocks.putAll(ResourceUtils.loadDir(FileToIdConverter.json("config/blockmaps/entity"), manager, StateLookup::new, id -> {
            return BuiltInRegistries.ENTITY_TYPE.getOptional(id.withPath(p -> p.replace("config/blockmaps/entity/", "").replace(".json", ""))).orElse(null);
        }, entries -> {
            Lookup<BlockState> lookup = new Lookup<>();
            return lookup.load(entries, globalBlocks()) ? lookup : null;
        }));
        hasConfigurations |= !blocks.isEmpty();
        hasConfigurations |= ResourceUtils.forEach(BIOME_MAP, manager, biomes()::load);
        hasConfigurations |= golems().load(ResourceUtils.load(GOLEM_MAP, manager, GolemLookup::new));
        hasConfigurations |= primitives().load(ResourceUtils.load(PRIMITIVE_MAP, manager, PrimitiveLookup::new));
        hasConfigurations |= ResourceUtils.forEach(LOCOMOTION_MAP, manager, locomotions()::load);
        var acoustics = ResourceUtils.loadAll(ACOUSTICS, manager, Acoustic.CODEC);
        hasConfigurations |= !acoustics.isEmpty();
        acoustics.forEach((id, acoustic) -> {
            acoustics().addAcoustic(id.getPath(), acoustic);
        });
        hasConfigurations |= ResourceUtils.forEach(VARIATOR, manager, variator()::load);
        return hasConfigurations;
    }

    @Override
    public void writeToReport(boolean full, JsonObjectWriter writer, Map<String, SoundType> groups) throws IOException {
        writer.object(() -> {
            writer.object("blocks", () -> StateLookup.writeToReport(globalBlocks(), full, writer, groups));
            writer.object("golems", () -> GolemLookup.writeToReport(golems(), full, writer, groups));
            writer.object("entities", () -> locomotions().writeToReport(full, writer, groups));
            writer.object("primitives", () -> PrimitiveLookup.writeToReport(primitives(), full, writer, groups));
        });
    }
}
