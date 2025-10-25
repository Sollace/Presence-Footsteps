package eu.ha3.presencefootsteps.util;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public interface ResourceUtils {
    static boolean forEach(Identifier id, ResourceManager manager, Consumer<Reader> consumer) {
        return manager.getAllResources(id).stream().mapToInt(res -> {
            try (Reader stream = new InputStreamReader(res.getInputStream())) {
                consumer.accept(stream);
                return 1;
            } catch (Exception e) {
                PresenceFootsteps.logger.error("Error encountered loading resource " + id + " from pack" + res.getPackId(), e);
                return 0;
            }
        }).sum() > 0;
    }

    static <T> Stream<T> load(Identifier id, ResourceManager manager, Function<JsonObject, T> reader) {
        return load(id, manager.getAllResources(id).stream(), reader);
    }

    static <T> Stream<T> load(Identifier id, Stream<Resource> resources, Function<JsonObject, T> reader) {
        return resources.map(res -> {
            try (JsonReader stream = new JsonReader(new InputStreamReader(res.getInputStream()))) {
                return reader.apply(Streams.parse(stream).getAsJsonObject());
            } catch (Exception e) {
                PresenceFootsteps.logger.error("Error encountered loading resource " + id + " from pack" + res.getPackId(), e);
                return (T)null;
            }
        }).filter(Objects::nonNull);
    }
    static <T> Map<Identifier, T> loadAll(Identifier directory, ResourceManager manager, Codec<T> codec) {
        Map<Identifier, T> results = new HashMap<>();
        JsonDataLoader.load(manager, ResourceFinder.json(directory.getPath()), JsonOps.INSTANCE, codec, results);
        return results;
    }

    @SuppressWarnings("unchecked")
    static <T, K, V> Map<K, V> loadDir(ResourceFinder finder, ResourceManager manager,
            Function<JsonObject, T> reader,
            Function<Identifier, @Nullable K> keyMapper,
            Function<Stream<T>, @Nullable V> valueMapper) {
        return Map.ofEntries(finder.findAllResources(manager).entrySet().stream()
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
