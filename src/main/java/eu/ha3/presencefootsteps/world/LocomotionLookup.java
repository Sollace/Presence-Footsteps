package eu.ha3.presencefootsteps.world;

import eu.ha3.presencefootsteps.PFConfig;
import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.generator.Locomotion;
import eu.ha3.presencefootsteps.util.JsonObjectWriter;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;

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
        if (key instanceof Player player) {
            return Locomotion.forPlayer(player, config.getLocomotion());
        }
        return Locomotion.forLiving(key, values.getOrDefault(EntityType.getKey(key.getType()), Locomotion.BIPED));
    }

    @Override
    public void add(String key, JsonElement value) {
        Identifier id = Identifier.parse(key);

        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            PresenceFootsteps.logger.warn("Locomotion registered for unknown entity type " + id);
        }

        values.put(id, Locomotion.byName(value.getAsString().toUpperCase()));
    }

    @Override
    public boolean contains(Identifier key) {
        return values.containsKey(key);
    }

    @Override
    public void writeToReport(boolean full, JsonObjectWriter writer, Map<String, SoundType> groups) throws IOException {
        writer.each(BuiltInRegistries.ENTITY_TYPE, type -> {
            Identifier id = EntityType.getKey(type);
            if (full || !contains(id)) {
                if (type.create(Minecraft.getInstance().level, EntitySpawnReason.CHUNK_GENERATION) instanceof LivingEntity) {
                    writer.field(id.toString(), values.getOrDefault(id, Locomotion.NONE).name());
                }
            }
        });
    }
}
