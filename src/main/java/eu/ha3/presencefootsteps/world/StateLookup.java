package eu.ha3.presencefootsteps.world;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A state lookup that finds an association for a given block state within a specific substrate (or no substrate).
 *
 * @author Sollace
 */
public record StateLookup(Map<String, Bucket> substrates) implements Lookup<BlockState> {

    public StateLookup() {
        this(new Object2ObjectLinkedOpenHashMap<>());
    }

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

        Key k = Key.of(key, value);

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

        record Substrate(
                KeyList wildcards,
                Map<Identifier, Bucket> blocks,
                Map<Identifier, Bucket> tags) implements Bucket
        {

            Substrate(String substrate) {
                this(new KeyList(), new Object2ObjectLinkedOpenHashMap<>(), new Object2ObjectLinkedOpenHashMap<>());
            }

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
                final Key association = getTile(state).get(state);

                return association == Key.NULL
                        ? wildcards.findMatch(state)
                        : association;
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

        record Tile(Map<BlockState, Key> cache, KeyList keys) implements Bucket {

            public Tile() {
                this(new Object2ObjectLinkedOpenHashMap<>(), new KeyList());
            }

            Tile(Identifier id) {
                this(new Object2ObjectLinkedOpenHashMap<>(), new KeyList());
            }

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

    private record KeyList(Set<Key> priorityKeys, Set<Key> keys) {

        public KeyList() {
            this(new ObjectOpenHashSet<>(), new ObjectOpenHashSet<>());
        }

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

    private record Key(
            Identifier identifier,
            String substrate,
            Set<Attribute> properties,
            String value,
            boolean empty,
            boolean isTag,
            boolean isWildcard
    ) {

        public static final Key NULL = new Key();

        @Contract(value = "_, _ -> new", pure = true)
        public static @NotNull Key of(@NotNull String key, @NotNull String value) {
            final boolean isTag = key.indexOf('#') == 0;

            if (isTag) {
                key = key.replaceFirst("#", "");
            }

            final String id = key.split("[\\.\\[]")[0];
            final boolean isWildcard = id.indexOf('*') == 0;
            Identifier identifier = new Identifier("air");

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
            }

            key = key.replace(id, "");
            final String substrate = key.replaceFirst("\\[[^\\]]+\\]", "");
            String finalSubstrate = "";

            if (substrate.indexOf('.') > -1) {
                finalSubstrate = substrate.split("\\.")[1];
                key = key.replace(substrate, "");
            }

            final Set<Key.Attribute> properties = ObjectArrayList.of(
                         key.replace("[", "")
                            .replace("]", "")
                            .split(","))
                    .stream()
                    .filter(line -> line.indexOf('=') > -1)
                    .map(Key.Attribute::new)
                    .collect(ObjectOpenHashSet.toSet());

            final boolean empty = properties.isEmpty();

            return new Key(identifier, finalSubstrate, properties, value, empty, isTag, isWildcard);
        }

        private Key() {
            this(
                    new Identifier("air"),
                    "",
                    ObjectSets.emptySet(),
                    Emitter.UNASSIGNED,
                    true,
                    false,
                    false
            );
        }

        boolean matches(BlockState state) {

            if (empty) {
                return true;
            }

            Map<Property<?>, Comparable<?>> entries = state.getEntries();
            Set<Property<?>> keys = entries.keySet();

            for (Key.Attribute property : properties) {
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
                    + "[" + properties.stream().map(Key.Attribute::toString).collect(Collectors.joining()) + "]"
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

        private record Attribute(String name, String value) {
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
