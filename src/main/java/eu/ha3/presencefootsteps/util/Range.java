package eu.ha3.presencefootsteps.util;

import java.util.Random;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;

public record Range (float min, float max) {
    private static final Codec<Float> PERCENTAGE_CODEC = Codec.FLOAT.xmap(i -> i / 100F, i -> i * 100F);
    private static final Codec<Range> RANGE_CODEC = RecordCodecBuilder.create(i -> i.group(
        PERCENTAGE_CODEC.fieldOf("min").forGetter(Range::min),
        PERCENTAGE_CODEC.fieldOf("max").forGetter(Range::max)
    ).apply(i, Range::new));
    private static final Codec<Range> POINT_CODEC = PERCENTAGE_CODEC.xmap(Range::exactly, Range::min);
    public static final Codec<Range> CODEC = Codec.xor(POINT_CODEC, RANGE_CODEC).xmap(either -> Either.unwrap(either), i -> Mth.equal(i.min(), i.max()) ? Either.left(i) : Either.right(i));

    public static final Range DEFAULT = exactly(1);

    public static Range exactly(float value) {
        return new Range(value, value);
    }

    @Deprecated
    public Range read(String name, JsonObject json) {
        if ("volume".equals(name) && (json.has("vol") || json.has("vol_min") || json.has("vol_max"))) {
            return read("vol", json);
        }
        if (json.has(name)) {
            JsonElement element = json.get(name);
            if (element.isJsonObject()) {
                return new Range(
                    getPercentage(element.getAsJsonObject(), "min", min),
                    getPercentage(element.getAsJsonObject(), "max", max)
                );
            }
            return exactly(getPercentage(json, name, min));
        }

        return new Range(
                getPercentage(json, name + "_min", min),
                getPercentage(json, name + "_max", max)
        );
    }

    public float random(Random rand) {
        return MathUtil.randAB(rand, min, max);
    }

    public float on(float value) {
        return Mth.lerp(value, min, max);
    }

    private static float getPercentage(JsonObject object, String param, float fallback) {
        if (!object.has(param)) {
            return fallback;
        }
        return object.get(param).getAsFloat() / 100F;
    }
}
