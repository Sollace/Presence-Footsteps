package eu.ha3.presencefootsteps.sound;

import java.io.IOException;
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
import eu.ha3.presencefootsteps.world.EntityBlockLookup;
import eu.ha3.presencefootsteps.world.GolemLookup;
import eu.ha3.presencefootsteps.world.HeuristicStateLookup;
import eu.ha3.presencefootsteps.world.Index;
import eu.ha3.presencefootsteps.world.LocomotionLookup;
import eu.ha3.presencefootsteps.world.Lookup;
import eu.ha3.presencefootsteps.world.PrimitiveLookup;
import eu.ha3.presencefootsteps.world.StateLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public record Isolator (
        Variator variator,
        Index<Entity, Locomotion> locomotions,
        HeuristicStateLookup heuristics,
        Lookup<EntityType<?>> golems,
        EntityBlockLookup blocks,
        Index<Identifier, BiomeVarianceLookup.BiomeVariance> biomes,
        Lookup<SoundEvent> primitives,
        AcousticLibrary acoustics,
        boolean empty
    ) implements Reportable {
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
                new EntityBlockLookup(),
                new BiomeVarianceLookup(),
                new Lookup<>(),
                new AcousticsPlayer(new DelayedSoundPlayer(engine.soundPlayer)),
                true
        );
    }

    public Lookup<BlockState> blocks(EntityType<?> sourceType) {
        return blocks.getLookup(sourceType);
    }

    public Isolator load(ResourceManager manager) {
        return new Isolator(
                variator, locomotions, heuristics, golems, blocks, biomes, primitives, acoustics,
                !(
                        blocks.load(manager)
                        | ResourceUtils.forEach(BIOME_MAP, manager, biomes.createLoader())
                        | golems.load(ResourceUtils.load(GOLEM_MAP, manager, GolemLookup::new))
                        | primitives.load(ResourceUtils.load(PRIMITIVE_MAP, manager, PrimitiveLookup::new))
                        | ResourceUtils.forEach(LOCOMOTION_MAP, manager, locomotions.createLoader())
                        | acoustics.load(ResourceUtils.loadAll(ACOUSTICS, manager, Acoustic.CODEC))
                        | ResourceUtils.forEach(VARIATOR, manager, variator::load)
                )
        );
    }

    @Override
    public void writeToReport(boolean full, JsonObjectWriter writer, Map<String, SoundType> groups) throws IOException {
        writer.object(() -> {
            writer.object("blocks", () -> StateLookup.writeToReport(blocks(EntityTypes.PLAYER), full, writer, groups));
            writer.object("golems", () -> GolemLookup.writeToReport(golems(), full, writer, groups));
            writer.object("entities", () -> locomotions().writeToReport(full, writer, groups));
            writer.object("primitives", () -> PrimitiveLookup.writeToReport(primitives(), full, writer, groups));
        });
    }
}
