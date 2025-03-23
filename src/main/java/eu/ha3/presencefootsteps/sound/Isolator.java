package eu.ha3.presencefootsteps.sound;

import java.io.IOException;
import java.util.Map;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.config.Variator;
import eu.ha3.presencefootsteps.sound.acoustics.AcousticLibrary;
import eu.ha3.presencefootsteps.sound.acoustics.AcousticsFile;
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
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public record Isolator (
        Variator variator,
        Index<Entity, Locomotion> locomotions,
        HeuristicStateLookup heuristics,
        Lookup<EntityType<?>> golems,
        Lookup<BlockState> blocks,
        Index<Identifier, BiomeVarianceLookup.BiomeVariance> biomes,
        Lookup<SoundEvent> primitives,
        AcousticLibrary acoustics
    ) implements Reportable {
    private static final Identifier BLOCK_MAP = PresenceFootsteps.id("config/blockmap.json");
    private static final Identifier BIOME_MAP = PresenceFootsteps.id("config/biomevariancemap.json");
    private static final Identifier GOLEM_MAP = PresenceFootsteps.id("config/golemmap.json");
    private static final Identifier LOCOMOTION_MAP = PresenceFootsteps.id("config/locomotionmap.json");
    private static final Identifier PRIMITIVE_MAP = PresenceFootsteps.id("config/primitivemap.json");
    public static final Identifier ACOUSTICS = PresenceFootsteps.id("config/acoustics.json");
    private static final Identifier VARIATOR = PresenceFootsteps.id("config/variator.json");

    public Isolator(SoundEngine engine) {
        this(new Variator(),
                new LocomotionLookup(engine.getConfig()),
                new HeuristicStateLookup(),
                new Lookup<>(),
                new Lookup<>(),
                new BiomeVarianceLookup(),
                new Lookup<>(),
                new AcousticsPlayer(new DelayedSoundPlayer(engine.soundPlayer))
        );
    }

    public boolean load(ResourceManager manager) {
        boolean hasConfigurations = false;
        hasConfigurations |= blocks().load(ResourceUtils.load(BLOCK_MAP, manager, StateLookup::new));
        hasConfigurations |= ResourceUtils.forEach(BIOME_MAP, manager, biomes()::load);
        hasConfigurations |= golems().load(ResourceUtils.load(GOLEM_MAP, manager, GolemLookup::new));
        hasConfigurations |= primitives().load(ResourceUtils.load(PRIMITIVE_MAP, manager, PrimitiveLookup::new));
        hasConfigurations |= ResourceUtils.forEach(LOCOMOTION_MAP, manager, locomotions()::load);
        hasConfigurations |= ResourceUtils.forEach(ACOUSTICS, manager, reader -> AcousticsFile.read(reader, acoustics()::addAcoustic, false));
        hasConfigurations |= ResourceUtils.forEach(VARIATOR, manager, variator()::load);
        return hasConfigurations;
    }

    @Override
    public void writeToReport(boolean full, JsonObjectWriter writer, Map<String, BlockSoundGroup> groups) throws IOException {
        writer.object(() -> {
            writer.object("blocks", () -> StateLookup.writeToReport(blocks(), full, writer, groups));
            writer.object("golems", () -> GolemLookup.writeToReport(golems(), full, writer, groups));
            writer.object("entities", () -> locomotions().writeToReport(full, writer, groups));
            writer.object("primitives", () -> PrimitiveLookup.writeToReport(primitives(), full, writer, groups));
        });
    }
}
