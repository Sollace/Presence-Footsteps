package eu.ha3.presencefootsteps.sound.acoustics;

import java.io.Reader;
import java.util.Map;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.util.Range;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * A JSON parser that creates a Library of Acoustics.
 *
 * @author Hurry
 */
public class AcousticsJsonParser {
    private final int ENGINEVERSION = 1;

    private String soundRoot = "";

    private static final Map<String, AcousticFactory> factories = new Object2ObjectOpenHashMap<>(6);

    private final Range defaultVolume = new Range(1);
    private final Range defaultPitch = new Range(1);

    static {
        factories.put("basic", VaryingAcoustic::new);
        factories.put("events", EventSelectorAcoustics::new);
        factories.put("simultaneous", SimultaneousAcoustic::new);
        factories.put("delayed", DelayedAcoustic::new);
        factories.put("probability", WeightedAcoustic::fromJson);
        factories.put("chance", ChanceAcoustic::fromJson);
    }

    private final AcousticLibrary lib;

    public AcousticsJsonParser(AcousticLibrary lib) {
        this.lib = lib;
    }

    public void parse(Reader reader) {
        try {
            doParse(reader);
        } catch (JsonParseException e) {
            PresenceFootsteps.logger.error("Error whilst loading acoustics", e);
        }
    }

    private void doParse(Reader reader) throws JsonParseException {
        soundRoot = "";
        defaultVolume.on(1);
        defaultPitch.on(1);

        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        if (!"library".equals(json.get("type").getAsString())) {
            throw new JsonParseException("Invalid type: \"library\"");
        }

        if (json.get("engineversion").getAsInt() != ENGINEVERSION) {
            throw new JsonParseException("Unrecognised Engine version: " + ENGINEVERSION + " expected, got "
                    + json.get("engineversion").getAsInt());
        }

        if (!json.has("contents")) {
            throw new JsonParseException("Empty contents");
        }

        if (json.has("soundroot")) {
            soundRoot = json.get("soundroot").getAsString();
        }

        if (json.has("defaults")) {
            JsonObject defaults = json.getAsJsonObject("defaults");
            defaultVolume.read("vol", defaults, this);
            defaultPitch.read("pitch", defaults, this);
        }

        json.getAsJsonObject("contents").entrySet().forEach(element -> {
            lib.addAcoustic(element.getKey(), solveAcoustic(element.getValue(), "events"));
        });
    }

    public Acoustic solveAcoustic(JsonElement unsolved) throws JsonParseException {
        return solveAcoustic(unsolved, "basic");
    }

    private Acoustic solveAcoustic(JsonElement unsolved, String defaultUnassigned) throws JsonParseException {
        Acoustic ret = null;

        if (unsolved.isJsonObject()) {
            ret = solveAcousticsCompound(unsolved.getAsJsonObject(), defaultUnassigned);
        } else if (unsolved.isJsonArray()) {
            ret = new SimultaneousAcoustic(unsolved.getAsJsonArray(), this);
        } else if (unsolved.isJsonPrimitive() && unsolved.getAsJsonPrimitive().isString()) {
            ret = new VaryingAcoustic(unsolved.getAsString(), this);
        }

        if (ret == null) {
            throw new JsonParseException("Unresolved Json element: \r\n" + unsolved.toString());
        }

        return ret;
    }

    private Acoustic solveAcousticsCompound(JsonObject unsolved, String defaultUnassigned) throws JsonParseException {

        String type = unsolved.has("type") ? unsolved.get("type").getAsString() : defaultUnassigned;

        if (!factories.containsKey(type)) {
            throw new JsonParseException("Invalid type for acoustic `" + type + "`");
        }

        return factories.get(type).create(unsolved, this);
    }

    public Range getVolumeRange() {
        return defaultVolume;
    }

    public Range getPitchRange() {
        return defaultPitch;
    }

    public String getSoundName(String soundName) {
        if (soundName.charAt(0) != '@') {
            return soundRoot + soundName;
        }

        return soundName.replace("@", "");
    }

    public float getPercentage(JsonObject object, String param) {
        return object.get(param).getAsFloat() / 100F;
    }

    public interface AcousticFactory {
        Acoustic create(JsonObject json, AcousticsJsonParser context);
    }
}
