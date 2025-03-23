package eu.ha3.presencefootsteps.util;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;

import eu.ha3.presencefootsteps.PresenceFootsteps;
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
        return manager.getAllResources(id).stream().map(res -> {
            try (JsonReader stream = new JsonReader(new InputStreamReader(res.getInputStream()))) {
                return reader.apply(Streams.parse(stream).getAsJsonObject());
            } catch (Exception e) {
                PresenceFootsteps.logger.error("Error encountered loading resource " + id + " from pack" + res.getPackId(), e);
                return (T)null;
            }
        }).filter(Objects::nonNull);
    }
}
