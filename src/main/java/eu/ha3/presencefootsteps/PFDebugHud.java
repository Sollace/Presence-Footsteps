package eu.ha3.presencefootsteps;

import java.util.*;

import eu.ha3.presencefootsteps.api.DerivedBlock;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import eu.ha3.presencefootsteps.sound.generator.Locomotion;
import eu.ha3.presencefootsteps.world.PrimitiveLookup;
import eu.ha3.presencefootsteps.world.SoundsKey;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

public class PFDebugHud {

    private final SoundEngine engine;

    private final List<String> list = new ArrayList<>();

    PFDebugHud(SoundEngine engine) {
        this.engine = engine;
    }

    public void render(HitResult blockHit, HitResult fluidHit, List<String> finalList) {
        MinecraftClient client = MinecraftClient.getInstance();

        list.add("");
        list.add(Formatting.UNDERLINE + "Presence Footsteps " + FabricLoader.getInstance().getModContainer("presencefootsteps").get().getMetadata().getVersion());

        PFConfig config = engine.getConfig();
        list.add(String.format("Enabled: %s, Multiplayer: %s, Running: %s", config.getEnabled(), config.getEnabledMP(), engine.isRunning(client)));
        list.add(String.format("Volume: Global[G: %s%%, W: %s%%, F: %s%%]",
                config.getGlobalVolume(),
                config.wetSoundsVolume,
                config.foliageSoundsVolume
        ));
        list.add(String.format("Entities[H: %s%%, P: %s%%], Players[U: %s%%, T: %s%% ]",
                config.hostileEntitiesVolume,
                config.passiveEntitiesVolume,
                config.clientPlayerVolume,
                config.otherPlayerVolume
        ));
        list.add(String.format("Stepping Mode: %s, Targeting Mode: %s, Footwear: %s", config.getLocomotion() == Locomotion.NONE
                ? String.format("AUTO (%sDETECTED %s%s)", Formatting.BOLD, Locomotion.forPlayer(client.player, Locomotion.NONE), Formatting.RESET)
                : config.getLocomotion(), config.getEntitySelector(), config.getEnabledFootwear()));
        list.add(String.format("Data Loaded: B%s P%s G%s",
                engine.getIsolator().globalBlocks().getSubstrates().size(),
                engine.getIsolator().primitives().getSubstrates().size(),
                engine.getIsolator().golems().getSubstrates().size()
        ));
        list.add(String.format("Has Resource Pack: %s%s", engine.hasData() ? Formatting.GREEN : Formatting.RED, engine.hasData()));

        insertAt(list, finalList, "Targeted Block: ", -1);

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult)blockHit).getBlockPos();
            BlockState state = client.world.getBlockState(pos);

            list.add("");
            list.add(Formatting.UNDERLINE + "Targeted Block Sounds Like");
            BlockState base = DerivedBlock.getBaseOf(state);
            if (!base.isAir()) {
                list.add(Registries.BLOCK.getId(base.getBlock()).toString());
            }
            list.add(String.format(Locale.ENGLISH, "Primitive Key: %s", PrimitiveLookup.getKey(state.getSoundGroup())));
            BlockPos above = pos.up();
            boolean hasRain = client.world.isRaining() && client.world.getBiome(above).value().getPrecipitation(above, client.world.getSeaLevel()) == Biome.Precipitation.RAIN;
            boolean hasLava = client.world.getBlockState(above).getFluidState().isIn(FluidTags.LAVA);
            boolean hasWater = client.world.hasRain(above)
                    || state.getFluidState().isIn(FluidTags.WATER)
                    || client.world.getBlockState(above).getFluidState().isIn(FluidTags.WATER);
            list.add("Surface Condition: " + (
                    hasLava ? Formatting.RED + "LAVA"
                            : hasWater ? Formatting.BLUE + "WET"
                            : hasRain ? Formatting.GRAY + "SHELTERED" : Formatting.GRAY + "DRY"
            ));
            renderSoundList("Step Sounds[B]", engine.getIsolator().globalBlocks().getAssociations(state), list);
            renderSoundList("Step Sounds[P]", engine.getIsolator().primitives().getAssociations(state.getSoundGroup().getStepSound()), list);
            list.add("");

            insertAt(list, finalList, "Targeted Block: ", 1);
        }

        if (client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() != null) {
            list.add(String.format("Targeted Entity Step Mode: %s", engine.getIsolator().locomotions().lookup(ehr.getEntity())));
            renderSoundList("Step Sounds[G]", engine.getIsolator().golems().getAssociations(ehr.getEntity().getType()), list);
            insertAt(list, finalList, "Targeted Entity", 3);
        }
    }

    private static void insertAt(List<String> values, List<String> destination, String target, int offset) {
        int i = 0;
        for (; i < destination.size(); i++) {
            if (destination.get(i).indexOf(target) != -1) {
                break;
            }
        }

        destination.addAll(MathHelper.clamp(i + offset, 0, destination.size()), values);
        values.clear();
    }

    private void renderSoundList(String title, Map<String, SoundsKey> sounds, List<String> list) {
        if (sounds.isEmpty()) {
            return;
        }
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
            return;
        }

        if (sounds.isEmpty()) {
            list.add(SoundsKey.UNASSIGNED.raw());
        } else {
            sounds.forEach((key, value) -> {
                list.add((key.isEmpty() ? "default" : key) + ": " + value.raw());
            });
        }
    }
}
