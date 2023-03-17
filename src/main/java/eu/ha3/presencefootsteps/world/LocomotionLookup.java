package eu.ha3.presencefootsteps.world;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.generator.Locomotion;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Map;

public class LocomotionLookup implements Index<Entity, Locomotion> {

    private final Map<Identifier, Locomotion> values = new Object2ObjectLinkedOpenHashMap<>();

    @Override
    public Locomotion lookup(Entity key) {
        if (key instanceof PlayerEntity) {
            return Locomotion.forPlayer((PlayerEntity)key, Locomotion.NONE);
        }
        return Locomotion.forLiving(key, values.getOrDefault(EntityType.getId(key.getType()), Locomotion.BIPED));
    }

    @Override
    public void add(String key, String value) {
        Identifier id = new Identifier(key);

        if (!Registries.ENTITY_TYPE.containsId(id)) {
            PresenceFootsteps.logger.warn("Locomotion registered for unknown entity type " + id);
        }

        values.put(id, Locomotion.byName(value.toUpperCase()));
    }

    @Override
    public boolean contains(Identifier key) {
        return values.containsKey(key);
    }
}
