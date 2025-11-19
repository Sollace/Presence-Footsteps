package eu.ha3.presencefootsteps.sound.acoustics;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.entity.LivingEntity;

/**
 * Something that has the ability to play sounds.
 *
 * @author Hurry
 */
public interface Acoustic {
    Map<String, MapCodec<? extends Acoustic>> TYPES = new Object2ObjectOpenHashMap<>();
    MapCodec<Acoustic> MAP_CODEC = Codec.STRING.dispatchMap(Acoustic::type, TYPES::get);
    Codec<Acoustic> CODEC = Codec.xor(Codec.lazyInitialized(() -> SimultaneousAcoustic.CODEC.xmap(i -> (Acoustic)i, i-> (SimultaneousAcoustic)i)), MAP_CODEC.codec()).xmap(
            either -> Either.unwrap(either),
            acoustic -> acoustic.type() == "simultaneous" ? Either.left(acoustic) : Either.right(acoustic)
    );
    String BASIC = register("basic", VaryingAcoustic.CODEC);
    String EVENTS = register("events", EventSelectorAcoustics.CODEC);
    String SIMULTANEOUS = register("simultaneous", SimultaneousAcoustic.MAP_CODEC);
    String DELAYED = register("delayed", DelayedAcoustic.CODEC);
    String PROBABILITY = register("probability", WeightedAcoustic.CODEC);
    String CHANCE = register("chance", ChanceAcoustic.CODEC);

    @Deprecated
    Map<String, Serializer> FACTORIES = Object2ObjectMaps.unmodifiable(new Object2ObjectOpenHashMap<>(new String[] {
            "basic",
            "events",
            "simultaneous",
            "delayed",
            "probability",
            "chance"
    }, new Serializer[] {
            VaryingAcoustic.FACTORY,        // basic
            EventSelectorAcoustics.FACTORY, // events
            SimultaneousAcoustic.FACTORY,   // simultaneous
            DelayedAcoustic.FACTORY,        // delayed
            WeightedAcoustic.FACTORY,       // probability
            ChanceAcoustic.FACTORY          // chance
    }));

    static <A extends Acoustic> String register(String type, MapCodec<A> codec) {
        TYPES.put(type, codec);
        return type;
    }

    @Deprecated
    static Acoustic read(AcousticsFile context, JsonElement unsolved) throws JsonParseException {
        return read(context, unsolved, "basic");
    }

    @Deprecated
    static Acoustic read(AcousticsFile context, JsonElement json, String defaultUnassigned) throws JsonParseException {
        String type = getType(json, defaultUnassigned);
        return checked(checked(FACTORIES.get(type), () -> "Invalid type for acoustic `" + type + "`").create(json, context), () -> "Unresolved Json element: \r\n" + json.toString());
    }

    @Deprecated
    private static String getType(JsonElement unsolved, String defaultUnassigned) {
        if (unsolved.isJsonObject()) {
            JsonObject json = unsolved.getAsJsonObject();
            return json.has("type") ? json.get("type").getAsString() : defaultUnassigned;
        }

        if (unsolved.isJsonArray()) {
            return "simultaneous";
        }

        if (unsolved.isJsonPrimitive() && unsolved.getAsJsonPrimitive().isString()) {
            return "basic";
        }

        return "";
    }

    @Deprecated
    private static <T> T checked(T t, Supplier<String> message) throws JsonParseException {
        if (t == null) {
            throw new JsonParseException(message.get());
        }
        return t;
    }

    /**
     * Plays a sound.
     */
    void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions);

    String type();

    @Deprecated
    public interface Serializer {
        Acoustic create(JsonElement json, AcousticsFile context);

        static Serializer ofJsObject(BiFunction<JsonObject, AcousticsFile, Acoustic> factory) {
            return (json, context) -> factory.apply(json.getAsJsonObject(), context);
        }
    }
}