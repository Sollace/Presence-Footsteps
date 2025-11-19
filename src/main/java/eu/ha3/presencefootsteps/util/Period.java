package eu.ha3.presencefootsteps.util;

import java.util.Random;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import eu.ha3.presencefootsteps.sound.Options;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

public record Period(long min, long max) implements Options {
    private static final Codec<Period> RANGE_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("min").forGetter(Period::min),
            Codec.LONG.fieldOf("max").forGetter(Period::max)
    ).apply(i, Period::of));
    private static final Codec<Period> POINT_CODEC = Codec.LONG.xmap(Period::of, Period::min);
    public static final Codec<Period> CODEC = Codec.xor(POINT_CODEC, RANGE_CODEC).xmap(Either::unwrap, period -> period.min() == period.max() ? Either.left(period) : Either.right(period));
    public static final Period ZERO = new Period(0, 0);

    public static Period of(long value) {
        return of(value, value);
    }

    public static Period of(long min, long max) {
        return (min == max && max == 0) ? ZERO : new Period(min, max);
    }

    @Deprecated
    public static Period fromJson(JsonObject json, String key) {
        if (json.has(key)) {
            return Period.of(json.get(key).getAsLong());
        }

        return Period.of(
                GsonHelper.getAsLong(json, key + "_min", 0),
                GsonHelper.getAsLong(json, key + "_max", 0)
        );
    }

    public float random(Random rand) {
        return MathUtil.randAB(rand, min, max);
    }

    public float on(float value) {
        return Mth.lerp(value, min, max);
    }

    @Override
    public boolean containsKey(String option) {
        return "delay_min".equals(option)
            || "delay_max".equals(option);
    }

    @Override
    public float get(String option) {
        return "delay_min".equals(option) ? min
             : "delay_max".equals(option) ? max
             : 0;
    }
}
