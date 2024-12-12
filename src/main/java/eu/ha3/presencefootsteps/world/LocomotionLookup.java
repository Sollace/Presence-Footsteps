package eu.ha3.presencefootsteps.world;

import eu.ha3.presencefootsteps.PFConfig;
import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.generator.Locomotion;
import eu.ha3.presencefootsteps.util.JsonObjectWriter;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.Map;

import com.google.gson.JsonElement;

public class LocomotionLookup implements Index<Entity, Locomotion> {
    private final Map<Identifier, Locomotion> values = new Object2ObjectLinkedOpenHashMap<>();

    private final PFConfig config;

    public LocomotionLookup(PFConfig config) {
        this.config = config;
    }

    @Override
    public Locomotion lookup(Entity key) {
        if (key instanceof PlayerEntity player) {
            return Locomotion.forPlayer(player, config.getLocomotion());
        }
        return Locomotion.forLiving(key, values.getOrDefault(EntityType.getId(key.getType()), Locomotion.BIPED));
    }

    @Override
    public void add(String key, JsonElement value) {
        Identifier id = Identifier.of(key);

        if (!Registries.ENTITY_TYPE.containsId(id)) {
            PresenceFootsteps.logger.warn("Locomotion registered for unknown entity type " + id);
        }

        values.put(id, Locomotion.byName(value.getAsString().toUpperCase()));
    }

    @Override
    public boolean contains(Identifier key) {
        return values.containsKey(key);
    }

    @Override
    public void writeToReport(boolean full, JsonObjectWriter writer, Map<String, BlockSoundGroup> groups) throws IOException {
        writer.each(Registries.ENTITY_TYPE, type -> {
            Identifier id = EntityType.getId(type);
            if (full || !contains(id)) {
                if (type.create(MinecraftClient.getInstance().world) instanceof LivingEntity) {
                    writer.field(id.toString(), values.getOrDefault(id, Locomotion.NONE).name());
                }
            }
        });
    }
}
