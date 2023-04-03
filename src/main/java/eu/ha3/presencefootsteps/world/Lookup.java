package eu.ha3.presencefootsteps.world;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface Lookup<T> extends Loadable {

    String EMPTY_SUBSTRATE = "";
    String CARPET_SUBSTRATE = "carpet";
    String WET_SUBSTRATE = "wet";
    String FENCE_SUBSTRATE = "bigger";
    String FOLIAGE_SUBSTRATE = "foliage";
    String MESSY_SUBSTRATE = "messy";

    /**
     * This will return the appropriate association for the given state and substrate.
     *
     * Returns Emitter.UNASSIGNED when no mapping exists,
     * or Emitter.NOT_EMITTER if such a mapping exists and produces no sound.
     */
    String getAssociation(T state, String substrate);

    /**
     * Gets a set of all the substrates this map contains entries for.
     */
    Set<String> getSubstrates();

    /**
     * Gets all the associations for the given state.
     */
    default Map<String, String> getAssociations(T state) {
        final Map<String, String> result = new Object2ObjectOpenHashMap<>();

        for (String substrate : getSubstrates()) {
            String association = getAssociation(state, substrate);

            if (Emitter.isResult(association)) {
                result.put(substrate, association);
            }
        }

        return Object2ObjectMaps.unmodifiable(new Object2ObjectOpenHashMap<>(result));
    }

    /**
     * Returns true if this lookup contains a mapping for the given value.
     */
    boolean contains(T state);
}