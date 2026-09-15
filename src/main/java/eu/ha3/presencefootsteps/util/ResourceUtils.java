package eu.ha3.presencefootsteps.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;

public interface ResourceUtils {
    static boolean forEach(Identifier id, ResourceManager manager, Consumer<Reader> consumer) {
        return manager.getResourceStack(id).stream().mapToInt(res -> {
            try (Reader stream = new InputStreamReader(res.open())) {
                consumer.accept(stream);
                return 1;
            } catch (Exception e) {
                PresenceFootsteps.LOGGER.error("Error encountered loading resource " + id + " from pack" + res.sourcePackId(), e);
                return 0;
            }
        }).sum() > 0;
    }

    static <T> Stream<T> load(Identifier id, ResourceManager manager, Function<JsonObject, T> reader) {
        return load(id, manager.getResourceStack(id).stream(), reader);
    }

    static <T> Stream<T> load(Identifier id, Stream<Resource> resources, Function<JsonObject, T> reader) {
        return resources.map(res -> {
            try (Reader stream = res.openAsReader()) {
                return reader.apply(StrictJsonParser.parse(stream).getAsJsonObject());
            } catch (Exception e) {
                PresenceFootsteps.LOGGER.error("Error encountered loading resource " + id + " from pack" + res.sourcePackId(), e);
                return (T)null;
            }
        }).filter(Objects::nonNull);
    }

    static <T> Map<Identifier, T> loadAll(Identifier directory, ResourceManager manager, Codec<T> codec) {
        Map<Identifier, T> results = new HashMap<>();
        var lister = FileToIdConverter.json(directory.getPath());
        for (Entry<Identifier, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier id = lister.fileToId(location);

            try (Reader reader = entry.getValue().openAsReader()) {
                codec.parse(JsonOps.INSTANCE, StrictJsonParser.parse(reader)).ifSuccess(parsed -> {
                    Preconditions.checkState(results.putIfAbsent(id, parsed) == null, "Duplicate data file ignored with ID " + id);
                }).ifError(error -> PresenceFootsteps.LOGGER.error("Couldn't parse data file '{}' from '{}': {}", id, location, error));
            } catch (JsonParseException | IllegalArgumentException | IOException e) {
                PresenceFootsteps.LOGGER.error("Couldn't parse data file '{}' from '{}'", id, location, e);
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    static <T, K, V> Map<K, V> loadDir(FileToIdConverter finder, ResourceManager manager,
            Function<JsonObject, T> reader,
            Function<Identifier, @Nullable K> keyMapper,
            Function<Stream<T>, @Nullable V> valueMapper) {
        return Map.ofEntries(finder.listMatchingResourceStacks(manager).entrySet().stream()
                .map(entry -> {
                    K k = keyMapper.apply(entry.getKey());
                    if (k == null) {
                        return null;
                    }
                    V v = valueMapper.apply(load(entry.getKey(), entry.getValue().stream(), reader));
                    return v == null ? null : Map.entry(k, v);
                })
                .filter(Objects::nonNull)
                .toArray(Map.Entry[]::new));
    }
}
