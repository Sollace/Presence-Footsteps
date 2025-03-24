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
    private DataSegment<T> data;

    public boolean load(Stream<? extends DataSegment<T>> data, Lookup<T> parent) {
        return load(Stream.of(parent.data, UnionDataSegment.of(data)));
    }

    public boolean load(Stream<? extends DataSegment<T>> data) {
        this.data = UnionDataSegment.of(data);
        return !this.data.isEmpty();
    }

    /**
     * This will return the appropriate association for the given state and substrate.
     *
     * Returns Emitter.UNASSIGNED when no mapping exists,
     * or Emitter.NOT_EMITTER if such a mapping exists and produces no sound.
     */
    public SoundsKey getAssociation(T state, String substrate) {
        return data.getAssociation(state, substrate).orElse(SoundsKey.UNASSIGNED);
    }

    /**
     * Gets a set of all the substrates this map contains entries for.
     */
    public Set<String> getSubstrates() {
        return data.getSubstrates();
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
        return data.contains(state);
    }

    /**
     * Returns true if this lookup contains a mapping for the given value for a specific substrate.
     */
    public boolean contains(T state, String substrate) {
        return data.contains(state, substrate);
    }

    public interface DataSegment<T> {
        Optional<SoundsKey> getAssociation(T state, String substrate);

        Set<String> getSubstrates();

        boolean contains(T state);

        boolean contains(T state, String substrate);

        boolean isEmpty();
    }

    record UnionDataSegment<T>(List<? extends DataSegment<T>> entries, Set<String> substrates) implements DataSegment<T> {
        static final UnionDataSegment<?> EMPTY = new UnionDataSegment<>(List.of(), Set.of());

        @SuppressWarnings("unchecked")
        public static <T> DataSegment<T> of(Stream<? extends DataSegment<T>> entries) {
            var data = entries.filter(i -> !i.isEmpty()).toList().reversed();
            if (data.size() == 1) {
                return data.get(0);
            }
            if (data.size() == 0) {
                return (UnionDataSegment<T>)EMPTY;
            }
            var substrates = data.stream().flatMap(i -> i.getSubstrates().stream()).distinct().collect(Collectors.toUnmodifiableSet());

            return new UnionDataSegment<>(data, substrates);
        }

        @Override
        public Optional<SoundsKey> getAssociation(T state, String substrate) {
            for (var segment : entries) {
                Optional<SoundsKey> key = segment.getAssociation(state, substrate);
                if (key.isPresent()) {
                    return key;
                }
            }
            return Optional.empty();
        }

        @Override
        public Set<String> getSubstrates() {
            return substrates;
        }

        @Override
        public boolean contains(T state) {
            for (var segment : entries) {
                if (segment.contains(state)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean contains(T state, String substrate) {
            for (var segment : entries) {
                if (segment.contains(state, substrate)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return entries.isEmpty();
        }
    }
}