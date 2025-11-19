package eu.ha3.presencefootsteps.sound.acoustics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.util.Range;
import net.minecraft.resources.Identifier;

import java.io.Reader;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

/**
 * A JSON parser that creates a Library of Acoustics.
 *
 * @author Hurry
 */
public record AcousticsFile (
        Range defaultVolume,
        Range defaultPitch,
        String soundRoot
) {
    public static final Identifier FILE_LOCATION = PresenceFootsteps.id("config/acoustics.json");
    private static final int ENGINE_VERSION = 2;

    @Deprecated
    @Nullable
    public static AcousticsFile read(Reader reader, BiConsumer<String, Acoustic> consumer, boolean ignoreVersion) {
        try {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            AcousticsFile context = read(json, ignoreVersion);
            json.getAsJsonObject("contents").entrySet().forEach(element -> {
                consumer.accept(element.getKey(), Acoustic.read(context, element.getValue(), "events"));
            });
            return context;
        } catch (JsonParseException e) {
            PresenceFootsteps.logger.error("Error whilst loading acoustics", e);
        }
        return null;
    }

    @Deprecated
    private static AcousticsFile read(JsonObject json, boolean ignoreVersion) {
        expect("library".equals(json.get("type").getAsString()), "Invalid type: Expected \"library\" got \"" + json.get("type").getAsString() + "\"");
        expect(ignoreVersion || json.get("engineversion").getAsInt() == ENGINE_VERSION, "Unrecognised Engine version: " + ENGINE_VERSION + " expected, got " + json.get("engineversion").getAsInt());
        expect(json.has("contents"), "Empty contents");

        String soundRoot = "";

        if (json.has("soundroot")) {
            soundRoot = json.get("soundroot").getAsString();
        }

        if (json.has("defaults")) {
            JsonObject defaults = json.getAsJsonObject("defaults");
            return new AcousticsFile(
                    Range.DEFAULT.read("volume", defaults),
                    Range.DEFAULT.read("pitch", defaults),
                    soundRoot
            );
        }

        return new AcousticsFile(
                Range.DEFAULT,
                Range.DEFAULT,
                soundRoot
        );
    }

    @Deprecated
    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new JsonParseException(message);
        }
    }

    @Deprecated
    public String getSoundName(String soundName) {
        if (soundName.charAt(0) != '@') {
            return soundRoot + soundName;
        }

        return soundName.replace("@", "");
    }
}
