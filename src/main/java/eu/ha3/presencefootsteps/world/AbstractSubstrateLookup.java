package eu.ha3.presencefootsteps.world;

import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.util.Identifier;

abstract class AbstractSubstrateLookup<T> implements Lookup<T> {
    private final Map<String, Map<Identifier, SoundsKey>> substrates = new Object2ObjectLinkedOpenHashMap<>();

    protected abstract Identifier getId(T key);

    @Override
    public SoundsKey getAssociation(@Nullable T key, String substrate) {
        if (key == null) {
            return SoundsKey.UNASSIGNED;
        }
        final Identifier id = getId(key);
        return getSubstrateMap(id, substrate).getOrDefault(id, SoundsKey.UNASSIGNED);
    }

    @Nullable
    protected Map<Identifier, SoundsKey> getSubstrateMap(Identifier id, String substrate) {
        Map<Identifier, SoundsKey> primitives = substrates.get(substrate);
        if (primitives != null) {
            return primitives;
        }

        // check for break sound
        primitives = substrates.get("break_" + id.getPath());

        if (primitives != null) {
            return primitives;
        }

        // Check for default
        return substrates.getOrDefault(Substrates.DEFAULT, Map.of());
    }

    @Override
    public Set<String> getSubstrates() {
        return substrates.keySet();
    }

    @Override
    public void add(String key, JsonElement value) {
        final String[] split = key.trim().split("@");
        final String primitive = split[0];
        final String substrate = split.length > 1 ? split[1] : Substrates.DEFAULT;

        substrates
            .computeIfAbsent(substrate, s -> new Object2ObjectLinkedOpenHashMap<>())
            .put(Identifier.of(primitive), SoundsKey.of(value.getAsString()));
    }

    @Override
    public boolean contains(T key) {
        final Identifier primitive = getId(key);

        for (var primitives : substrates.values()) {
            if (primitives.containsKey(primitive)) {
                return true;
            }
        }
        return false;
    }
}
