package eu.ha3.presencefootsteps;

import java.util.*;

import org.jetbrains.annotations.Nullable;

import eu.ha3.presencefootsteps.api.DerivedBlock;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import eu.ha3.presencefootsteps.sound.generator.Locomotion;
import eu.ha3.presencefootsteps.world.PrimitiveLookup;
import eu.ha3.presencefootsteps.world.SoundsKey;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.hud.debug.DebugHudEntry;
import net.minecraft.client.gui.hud.debug.DebugHudLines;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;

public class PFDebugHud implements DebugHudEntry {
    public static final Identifier ID = PresenceFootsteps.id("hud");

    private final SoundEngine engine;

    PFDebugHud(SoundEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean canShow(boolean reducedDebugInfo) {
        return true;
    }

    @Override
    public void render(DebugHudLines finalList, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
        MinecraftClient client = MinecraftClient.getInstance();

        PFConfig config = engine.getConfig();

        finalList.addLinesToSection(DebugHudEntries.SOUND_MOOD, List.of(
                "",
                Formatting.UNDERLINE + "Presence Footsteps " + FabricLoader.getInstance().getModContainer("presencefootsteps").get().getMetadata().getVersion(),
                String.format("Enabled: %s, Multiplayer: %s, Running: %s", config.getEnabled(), config.getEnabledMP(), engine.isRunning(client)),
                String.format("Volume: Global[G: %s%%, W: %s%%, F: %s%%]",
                        config.getGlobalVolume(),
                        config.wetSoundsVolume,
                        config.foliageSoundsVolume
                ),
                String.format("Entities[H: %s%%, P: %s%%], Players[U: %s%%, T: %s%% ]",
                        config.hostileEntitiesVolume,
                        config.passiveEntitiesVolume,
                        config.clientPlayerVolume,
                        config.otherPlayerVolume
                ),
                String.format("Stepping Mode: %s, Targeting Mode: %s, Footwear: %s", config.getLocomotion() == Locomotion.NONE
                        ? String.format("AUTO (%sDETECTED %s%s)", Formatting.BOLD, Locomotion.forPlayer(client.player, Locomotion.NONE), Formatting.RESET)
                        : config.getLocomotion(), config.getEntitySelector(), config.getEnabledFootwear()),
                String.format("Data Loaded: B%s P%s G%s",
                        engine.getIsolator().globalBlocks().getSubstrates().size(),
                        engine.getIsolator().primitives().getSubstrates().size(),
                        engine.getIsolator().golems().getSubstrates().size()
                ),
                String.format("Has Resource Pack: %s%s", engine.hasData() ? Formatting.GREEN : Formatting.RED, engine.hasData())
        ));

        if (client.crosshairTarget instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = client.world.getBlockState(pos);
            BlockPos above = pos.up();

            BlockState base = DerivedBlock.getBaseOf(state);
            boolean hasRain = client.world.isRaining() && client.world.getBiome(above).value().getPrecipitation(above, client.world.getSeaLevel()) == Biome.Precipitation.RAIN;
            boolean hasLava = client.world.getBlockState(above).getFluidState().isIn(FluidTags.LAVA);
            boolean hasWater = client.world.hasRain(above)
                    || state.getFluidState().isIn(FluidTags.WATER)
                    || client.world.getBlockState(above).getFluidState().isIn(FluidTags.WATER);

            finalList.addLinesToSection(DebugHudEntries.LOOKING_AT_BLOCK, List.of("", Formatting.UNDERLINE + "Targeted Block Sounds Like"));

            if (!base.isAir()) {
                finalList.addLineToSection(DebugHudEntries.LOOKING_AT_BLOCK, Registries.BLOCK.getId(base.getBlock()).toString());
            }
            finalList.addLinesToSection(DebugHudEntries.LOOKING_AT_BLOCK, List.of(
                    String.format(Locale.ENGLISH, "Primitive Key: %s", PrimitiveLookup.getKey(state.getSoundGroup())),
                    "Surface Condition: " + (
                            hasLava ? Formatting.RED + "LAVA"
                                    : hasWater ? Formatting.BLUE + "WET"
                                    : hasRain ? Formatting.GRAY + "SHELTERED" : Formatting.GRAY + "DRY"
                    )
            ));
            finalList.addLinesToSection(DebugHudEntries.LOOKING_AT_BLOCK, renderSoundList("Step Sounds[B]", engine.getIsolator().globalBlocks().getAssociations(state)));
            finalList.addLinesToSection(DebugHudEntries.LOOKING_AT_BLOCK, renderSoundList("Step Sounds[P]", engine.getIsolator().primitives().getAssociations(state.getSoundGroup().getStepSound())));
            finalList.addLineToSection(DebugHudEntries.LOOKING_AT_BLOCK, "");
        }

        if (client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() != null) {
            finalList.addLineToSection(DebugHudEntries.LOOKING_AT_ENTITY, String.format("Targeted Entity Step Mode: %s", engine.getIsolator().locomotions().lookup(ehr.getEntity())));
            finalList.addLinesToSection(DebugHudEntries.LOOKING_AT_ENTITY, renderSoundList("Step Sounds[G]", engine.getIsolator().golems().getAssociations(ehr.getEntity().getType())));
        }
    }

    private List<String> renderSoundList(String title, Map<String, SoundsKey> sounds) {
        if (sounds.isEmpty()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        StringBuilder combinedList = new StringBuilder(Formatting.UNDERLINE + title + Formatting.RESET + ": [ ");
        boolean first = true;
        for (var entry : sounds.entrySet()) {
            if (!first) {
                combinedList.append(" / ");
            }
            first = false;

            if (!entry.getKey().isEmpty()) {
                combinedList.append(entry.getKey()).append(":");
            }
            combinedList.append(entry.getValue().raw());
        }
        combinedList.append(" ]");
        list.add(combinedList.toString());

        if (!list.isEmpty()) {
            return list;
        }

        if (sounds.isEmpty()) {
            list.add(SoundsKey.UNASSIGNED.raw());
        } else {
            sounds.forEach((key, value) -> {
                list.add((key.isEmpty() ? "default" : key) + ": " + value.raw());
            });
        }

        return list;
    }
}
