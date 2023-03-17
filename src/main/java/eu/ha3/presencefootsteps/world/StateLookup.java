package eu.ha3.presencefootsteps.world;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * A state lookup that finds an association for a given block state within a specific substrate (or no substrate).
 *
 * @author Sollace
 */
public class StateLookup implements Lookup<BlockState> {

    private final Map<String, Bucket> substrates = new Object2ObjectLinkedOpenHashMap<>();

    @Override
    public String getAssociation(BlockState state, String substrate) {
        return substrates.getOrDefault(substrate, Bucket.EMPTY).get(state).value;
    }

    @Override
    public void add(String key, String value) {
        if (!Emitter.isResult(value)) {
            PresenceFootsteps.logger.info("Skipping non-result value " + key + "=" + value);
            return;
        }

        Key k = new Key(key, value);

        substrates.computeIfAbsent(k.substrate, Bucket.Substrate::new).add(k);
    }

    @Override
    public Set<String> getSubstrates() {
        return substrates.keySet();
    }

    @Override
    public boolean contains(BlockState state) {
        for (Bucket substrate : substrates.values()) {
            if (substrate.contains(state)) {
                return true;
            }
        }

        return false;
    }

    private interface Bucket {

        Bucket EMPTY = state -> Key.NULL;

        default void add(Key key) {}

        Key get(BlockState state);

        default boolean contains(BlockState state) {
            return false;
        }

        final class Substrate implements Bucket {
            private final KeyList wildcards = new KeyList();
            private final Map<Identifier, Bucket> blocks = new Object2ObjectLinkedOpenHashMap<>();
            private final Map<Identifier, Bucket> tags = new Object2ObjectLinkedOpenHashMap<>();

            Substrate(String substrate) { }

            @Override
            public void add(Key key) {
                if (key.isWildcard) {
                    wildcards.add(key);
                } else {
                    (key.isTag ? tags : blocks).computeIfAbsent(key.identifier, Tile::new).add(key);
                }
            }

            @Override
            public Key get(BlockState state) {
                Key association = getTile(state).get(state);

                if (association == Key.NULL) {
                    return wildcards.findMatch(state);
                }
                return association;
            }

            @Override
            public boolean contains(BlockState state) {
                return getTile(state).contains(state);
            }

            private Bucket getTile(BlockState state) {
                return blocks.computeIfAbsent(Registries.BLOCK.getId(state.getBlock()), id -> {
                    for (Identifier tag : tags.keySet()) {
                        if (state.isIn(TagKey.of(RegistryKeys.BLOCK, tag))) {
                            return tags.get(tag);
                        }
                    }

                    return Bucket.EMPTY;
                });
            }
        }

        final class Tile implements Bucket {
            private final Map<BlockState, Key> cache = new Object2ObjectLinkedOpenHashMap<>();
            private final KeyList keys = new KeyList();

            Tile(Identifier id) { }

            @Override
            public void add(Key key) {
                keys.add(key);
            }

            @Override
            public Key get(BlockState state) {
                return cache.computeIfAbsent(state, keys::findMatch);
            }

            @Override
            public boolean contains(BlockState state) {
                return get(state) != Key.NULL;
            }
        }
    }

    private static final class KeyList {
        private final Set<Key> priorityKeys = new ObjectOpenHashSet<>();
        private final Set<Key> keys = new ObjectOpenHashSet<>();

        void add(Key key) {
            Set<Key> keys = getSetFor(key);
            keys.remove(key);
            keys.add(key);
        }

        private Set<Key> getSetFor(Key key) {
            return key.empty ? keys : priorityKeys;
        }

        public Key findMatch(BlockState state) {
            for (Key i : priorityKeys) {
                if (i.matches(state)) {
                    return i;
                }
            }
            for (Key i : keys) {
                if (i.matches(state)) {
                    return i;
                }
            }
            return Key.NULL;
        }
    }

    private static final class Key {
        public static final Key NULL = new Key();

        public final Identifier identifier;

        public final String substrate;

        private final Set<Attribute> properties;

        public final String value;

        private final boolean empty;

        public final boolean isTag;

        public final boolean isWildcard;

        private Key() {
            identifier = new Identifier("air");
            substrate = "";
            properties = Collections.emptySet();
            value = Emitter.UNASSIGNED;
            empty = true;
            isTag = false;
            isWildcard = false;
        }

        /*
         * minecraft:block[one=1,two=2].substrate
         * #minecraft:blanks[one=1,two=2].substrate
         */
        Key(String key, String value) {

            this.value = value;
            this.isTag = key.indexOf('#') == 0;

            if (isTag) {
                key = key.replaceFirst("#", "");
            }

            String id = key.split("[\\.\\[]")[0];

            isWildcard = id.indexOf('*') == 0;

            if (!isWildcard) {
                if (id.indexOf('^') > -1) {
                    identifier = new Identifier(id.split("\\^")[0]);
                    PresenceFootsteps.logger.warn("Metadata entry for " + key + "=" + value + " was ignored");
                } else {
                    identifier = new Identifier(id);
                }

                if (!isTag && !Registries.BLOCK.containsId(identifier)) {
                    PresenceFootsteps.logger.warn("Sound registered for unknown block id " + identifier);
                }
            } else {
                identifier = new Identifier("air");
            }

            key = key.replace(id, "");

            String substrate = key.replaceFirst("\\[[^\\]]+\\]", "");

            if (substrate.indexOf('.') > -1) {
                this.substrate = substrate.split("\\.")[1];

                key = key.replace(substrate, "");
            } else {
                this.substrate = "";
            }

            properties = Lists.newArrayList(key.replace("[", "").replace("]", "").split(","))
                .stream()
                .filter(line -> line.indexOf('=') > -1)
                .map(Attribute::new)
                .collect(Collectors.toSet());
            empty = properties.isEmpty();
        }

        boolean matches(BlockState state) {

            if (empty) {
                return true;
            }

            Map<Property<?>, Comparable<?>> entries = state.getEntries();
            Set<Property<?>> keys = entries.keySet();

            for (Attribute property : properties) {
                for (Property<?> key : keys) {
                    if (key.getName().equals(property.name)) {
                        Comparable<?> value = entries.get(key);

                        if (!Objects.toString(value).equalsIgnoreCase(property.value)) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }

        @Override
        public String toString() {
            return (isTag ? "#" : "") + identifier
                    + "[" + properties.stream().map(Attribute::toString).collect(Collectors.joining()) + "]"
                    + "." + substrate
                    + "=" + value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (empty ? 1231 : 1237);
            result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
            result = prime * result + (isTag ? 1231 : 1237);
            result = prime * result + (isWildcard ? 1231 : 1237);
            result = prime * result + ((properties == null) ? 0 : properties.hashCode());
            result = prime * result + ((substrate == null) ? 0 : substrate.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj != null && getClass() == obj.getClass()) && equals((Key) obj);
        }
        private boolean equals(Key other) {
            return isTag == other.isTag && isWildcard == other.isWildcard && empty == other.empty
                    && Objects.equals(identifier, other.identifier)
                    && Objects.equals(substrate, other.substrate)
                    && Objects.equals(properties, other.properties);
        }

        private record Attribute (String name, String value) {
            Attribute(String prop) {
                this(prop.split("="));
            }
            Attribute(String[] split) {
                this(split[0], split[1]);
            }
            @Override
            public String toString() {
                return name + "=" + value;
            }
        }
    }
}
