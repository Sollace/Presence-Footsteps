package eu.ha3.presencefootsteps.world;

import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public record PrimitiveLookup(Map<String, Map<Identifier, String>> substrates) implements Lookup<BlockSoundGroup> {

    public PrimitiveLookup() {
        this(new Object2ObjectLinkedOpenHashMap<>());
    }

    @Override
    public String getAssociation(BlockSoundGroup sounds, String substrate) {
        final Identifier id = sounds.getStepSound().getId();
        Map<Identifier, String> primitives = substrates.get(substrate);

        if (primitives == null) {
            // Check for break sound
            primitives = substrates.get("break_" + id.getPath());
        }

        if (primitives == null) {
            // Check for default
            primitives = substrates.get(EMPTY_SUBSTRATE);
        }

        if (primitives == null) {
            return Emitter.UNASSIGNED;
        }

        return primitives.getOrDefault(id, Emitter.UNASSIGNED);
    }

    @Override
    public Set<String> getSubstrates() {
        return substrates.keySet();
    }

    @Override
    public void add(String key, String value) {
        final String[] split = key.trim().split("@");
        final String primitive = split[0];
        final String substrate = split.length > 1 ? split[1] : EMPTY_SUBSTRATE;

        substrates
            .computeIfAbsent(substrate, s -> new Object2ObjectLinkedOpenHashMap<>())
            .put(new Identifier(primitive), value);
    }

    @Override
    public boolean contains(BlockSoundGroup sounds) {
        final Identifier primitive = sounds.getStepSound().getId();

        for (Map<Identifier, String> primitives : substrates.values()) {
            if (primitives.containsKey(primitive)) {
                return true;
            }
        }
        return false;
    }
}
