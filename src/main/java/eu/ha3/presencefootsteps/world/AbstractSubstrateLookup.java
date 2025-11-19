package eu.ha3.presencefootsteps.world;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.resources.Identifier;

abstract class AbstractSubstrateLookup<T> implements Lookup.DataSegment<T> {
    private final Map<String, Map<Identifier, Optional<SoundsKey>>> substrates = new Object2ObjectLinkedOpenHashMap<>();

    protected AbstractSubstrateLookup(JsonObject json) {
        json.entrySet().forEach(entry -> {
            final String[] split = entry.getKey().trim().split("@");
            final String primitive = split[0];
            final String substrate = split.length > 1 ? split[1] : Substrates.DEFAULT;

            substrates
                .computeIfAbsent(substrate, s -> new Object2ObjectLinkedOpenHashMap<>())
                .put(Identifier.parse(primitive), Optional.of(SoundsKey.of(entry.getValue().getAsString())));
        });
    }

    protected abstract Identifier getId(T key);

    @Override
    public Optional<SoundsKey> getAssociation(@Nullable T key, String substrate) {
        if (key == null) {
            return Optional.empty();
        }
        final Identifier id = getId(key);
        return getSubstrateMap(id, substrate).getOrDefault(id, Optional.empty());
    }

    @Nullable
    protected Map<Identifier, Optional<SoundsKey>> getSubstrateMap(Identifier id, String substrate) {
        Map<Identifier, Optional<SoundsKey>> primitives = substrates.get(substrate);
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
    public boolean contains(T key) {
        final Identifier primitive = getId(key);

        for (var primitives : substrates.values()) {
            if (primitives.containsKey(primitive)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(T key, String substrate) {
        return substrates.containsKey(substrate) && substrates.get(substrate).containsKey(getId(key));
    }

    @Override
    public boolean isEmpty() {
        return substrates.isEmpty();
    }
}
