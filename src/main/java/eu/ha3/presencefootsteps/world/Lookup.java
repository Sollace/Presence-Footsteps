package eu.ha3.presencefootsteps.world;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Lookup<T> {
    private List<DataSegment<T>> data = List.of();
    private Set<String> substrates = Set.of();

    public boolean load(Stream<DataSegment<T>> data) {
        this.data = data.toList().reversed();
        this.substrates = this.data.stream().flatMap(i -> i.getSubstrates().stream()).distinct().collect(Collectors.toUnmodifiableSet());
        return !this.data.isEmpty();
    }

    /**
     * This will return the appropriate association for the given state and substrate.
     *
     * Returns Emitter.UNASSIGNED when no mapping exists,
     * or Emitter.NOT_EMITTER if such a mapping exists and produces no sound.
     */
    public SoundsKey getAssociation(T state, String substrate) {
        for (var segment : data) {
            Optional<SoundsKey> key = segment.getAssociation(state, substrate);
            if (key.isPresent()) {
                return key.get();
            }
        }
        return SoundsKey.UNASSIGNED;
    }

    /**
     * Gets a set of all the substrates this map contains entries for.
     */
    public Set<String> getSubstrates() {
        return substrates;
    }

    /**
     * Gets all the associations for the given state.
     */
    public Map<String, SoundsKey> getAssociations(T state) {
        final Map<String, SoundsKey> result = new Object2ObjectOpenHashMap<>();

        for (String substrate : getSubstrates()) {
            SoundsKey association = getAssociation(state, substrate);

            if (association.isResult()) {
                result.put(substrate, association);
            }
        }

        return Object2ObjectMaps.unmodifiable(new Object2ObjectOpenHashMap<>(result));
    }

    /**
     * Returns true if this lookup contains a mapping for the given value.
     */
    public boolean contains(T state) {
        for (var segment : data) {
            if (segment.contains(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this lookup contains a mapping for the given value for a specific substrate.
     */
    public boolean contains(T state, String substrate) {
        for (var segment : data) {
            if (segment.contains(state, substrate)) {
                return true;
            }
        }
        return false;
    }

    public interface DataSegment<T> {
        Optional<SoundsKey> getAssociation(T state, String substrate);

        Set<String> getSubstrates();

        boolean contains(T state);

        boolean contains(T state, String substrate);
    }
}