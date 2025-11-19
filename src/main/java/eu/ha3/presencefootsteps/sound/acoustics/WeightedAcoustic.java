package eu.ha3.presencefootsteps.sound.acoustics;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.LivingEntity;

import java.util.Iterator;
import java.util.List;

/**
 *
 * An acoustic that can pick from more than one sound to play, each with their own relative
 * weighting for how often that sound is picked.
 *
 * @author Hurry
 *
 */
record WeightedAcoustic(
        List<Entry> entries
) implements Acoustic {
    public static final MapCodec<WeightedAcoustic> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Entry.CODEC.listOf().fieldOf("entries").forGetter(WeightedAcoustic::entries)
    ).apply(i, WeightedAcoustic::new));

    @SuppressWarnings("deprecation")
    @Deprecated
    static final Serializer FACTORY = Serializer.ofJsObject((json, context) -> {
        List<Entry> entries = new ObjectArrayList<>();
        Iterator<JsonElement> iter = json.getAsJsonArray(json.has("array") ? "array" : "entries").iterator();
        while (iter.hasNext()) {
            int weight = iter.next().getAsInt();

            if (!iter.hasNext()) {
                throw new JsonParseException("Probability has odd number of children!");
            }

            entries.add(new Entry(weight, Acoustic.read(context, iter.next())));
        }

        return new WeightedAcoustic(entries);
    });

    WeightedAcoustic {
        float total = 0;
        for (Entry entry : entries) {
            Preconditions.checkArgument(entry.weight >= 0, "A probability weight can't be negative");
            total += entry.weight;
        }
        if (total < 0) {
            Preconditions.checkArgument(total >= 0, "A probability weight can't be negative");
        }

        for (Entry entry : entries) {
            entry.threshold = entry.weight / total;
        }
    }

    @Override
    public String type() {
        return Acoustic.PROBABILITY;
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        final float rand = player.getRNG().nextFloat();
        int marker = -1;
        while (++marker < entries.size()) {
            if (entries.get(marker).threshold >= rand) {
                entries.get(marker).acoustic.playSound(player, location, event, inputOptions);
                return;
            }
        }
    }

    private static class Entry {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("weight", 1)
                    .validate(weight -> weight >= 0 ? DataResult.success(weight) : DataResult.error(() -> "A probability weight cannot be negative", weight))
                    .forGetter(o -> o.weight),
                Acoustic.CODEC.fieldOf("acoustic").forGetter(o -> o.acoustic)
        ).apply(i, Entry::new));
        private final Acoustic acoustic;
        private final int weight;
        private float threshold;

        Entry(int weight, Acoustic acoustic) {
            this.weight = weight;
            this.acoustic = acoustic;
        }
    }
}