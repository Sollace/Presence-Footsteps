package eu.ha3.presencefootsteps.world;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;

public class GolemLookup implements Lookup<EntityType<?>> {

    private final Map<String, Map<Identifier, String>> substrates = new Object2ObjectLinkedOpenHashMap<>();

    @Override
    public String getAssociation(EntityType<?> key, String substrate) {

        Map<Identifier, String> primitives = substrates.get(substrate);

        if (primitives == null) {
            // Check for default
            primitives = substrates.get(EMPTY_SUBSTRATE);
        }

        if (primitives == null) {
            return Emitter.UNASSIGNED;
        }

        return primitives.getOrDefault(EntityType.getId(key), Emitter.UNASSIGNED);
    }

    @Override
    public Set<String> getSubstrates() {
        return substrates.keySet();
    }

    @Override
    public void add(String key, String value) {
        String[] split = key.trim().split("@");

        String primitive = split[0];
        String substrate = split.length > 1 ? split[1] : EMPTY_SUBSTRATE;

        substrates
            .computeIfAbsent(substrate, s -> new Object2ObjectLinkedOpenHashMap<>())
            .put(new Identifier(primitive), value);
    }

    @Override
    public boolean contains(EntityType<?> key) {
        Identifier primitive = EntityType.getId(key);

        for (Map<Identifier, String> primitives : substrates.values()) {
            if (primitives.containsKey(primitive)) {
                return true;
            }
        }
        return false;
    }
}
