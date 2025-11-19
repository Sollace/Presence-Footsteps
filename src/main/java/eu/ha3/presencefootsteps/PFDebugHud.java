package eu.ha3.presencefootsteps;

import java.util.*;

import org.jetbrains.annotations.Nullable;

import eu.ha3.presencefootsteps.api.DerivedBlock;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import eu.ha3.presencefootsteps.sound.generator.Locomotion;
import eu.ha3.presencefootsteps.world.PrimitiveLookup;
import eu.ha3.presencefootsteps.world.SoundsKey;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class PFDebugHud implements DebugScreenEntry {
    public static final Identifier ID = PresenceFootsteps.id("hud");

    private final SoundEngine engine;

    PFDebugHud(SoundEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return true;
    }

    @Override
    public void display(DebugScreenDisplayer finalList, @Nullable Level world, @Nullable LevelChunk clientChunk, @Nullable LevelChunk chunk) {
        Minecraft client = Minecraft.getInstance();

        PFConfig config = engine.getConfig();

        finalList.addToGroup(DebugScreenEntries.SOUND_MOOD, List.of(
                "",
                ChatFormatting.UNDERLINE + "Presence Footsteps " + FabricLoader.getInstance().getModContainer("presencefootsteps").get().getMetadata().getVersion(),
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
                        ? String.format("AUTO (%sDETECTED %s%s)", ChatFormatting.BOLD, Locomotion.forPlayer(client.player, Locomotion.NONE), ChatFormatting.RESET)
                        : config.getLocomotion(), config.getEntitySelector(), config.getEnabledFootwear()),
                String.format("Data Loaded: B%s P%s G%s",
                        engine.getIsolator().globalBlocks().getSubstrates().size(),
                        engine.getIsolator().primitives().getSubstrates().size(),
                        engine.getIsolator().golems().getSubstrates().size()
                ),
                String.format("Has Resource Pack: %s%s", engine.hasData() ? ChatFormatting.GREEN : ChatFormatting.RED, engine.hasData())
        ));

        if (client.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = client.level.getBlockState(pos);
            BlockPos above = pos.above();

            BlockState base = DerivedBlock.getBaseOf(state);
            boolean hasRain = client.level.isRaining() && client.level.getBiome(above).value().getPrecipitationAt(above, client.level.getSeaLevel()) == Biome.Precipitation.RAIN;
            boolean hasLava = client.level.getBlockState(above).getFluidState().is(FluidTags.LAVA);
            boolean hasWater = client.level.isRainingAt(above)
                    || state.getFluidState().is(FluidTags.WATER)
                    || client.level.getBlockState(above).getFluidState().is(FluidTags.WATER);

            finalList.addToGroup(DebugScreenEntries.LOOKING_AT_BLOCK, List.of("", ChatFormatting.UNDERLINE + "Targeted Block Sounds Like"));

            if (!base.isAir()) {
                finalList.addToGroup(DebugScreenEntries.LOOKING_AT_BLOCK, BuiltInRegistries.BLOCK.getKey(base.getBlock()).toString());
            }
            finalList.addToGroup(DebugScreenEntries.LOOKING_AT_BLOCK, List.of(
                    String.format(Locale.ENGLISH, "Primitive Key: %s", PrimitiveLookup.getKey(state.getSoundType())),
                    "Surface Condition: " + (
                            hasLava ? ChatFormatting.RED + "LAVA"
                                    : hasWater ? ChatFormatting.BLUE + "WET"
                                    : hasRain ? ChatFormatting.GRAY + "SHELTERED" : ChatFormatting.GRAY + "DRY"
                    )
            ));
            finalList.addToGroup(DebugScreenEntries.LOOKING_AT_BLOCK, renderSoundList("Step Sounds[B]", engine.getIsolator().globalBlocks().getAssociations(state)));
            finalList.addToGroup(DebugScreenEntries.LOOKING_AT_BLOCK, renderSoundList("Step Sounds[P]", engine.getIsolator().primitives().getAssociations(state.getSoundType().getStepSound())));
            finalList.addToGroup(DebugScreenEntries.LOOKING_AT_BLOCK, "");
        }

        if (client.hitResult instanceof EntityHitResult ehr && ehr.getEntity() != null) {
            finalList.addToGroup(DebugScreenEntries.LOOKING_AT_ENTITY, String.format("Targeted Entity Step Mode: %s", engine.getIsolator().locomotions().lookup(ehr.getEntity())));
            finalList.addToGroup(DebugScreenEntries.LOOKING_AT_ENTITY, renderSoundList("Step Sounds[G]", engine.getIsolator().golems().getAssociations(ehr.getEntity().getType())));
        }
    }

    private List<String> renderSoundList(String title, Map<String, SoundsKey> sounds) {
        if (sounds.isEmpty()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        StringBuilder combinedList = new StringBuilder(ChatFormatting.UNDERLINE + title + ChatFormatting.RESET + ": [ ");
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
