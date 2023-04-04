package eu.ha3.presencefootsteps.sound.acoustics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
        @NotNull List<Acoustic> theAcoustics,
        float[] probabilityThresholds
) implements Acoustic {

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull WeightedAcoustic of(
            final @NotNull List<Acoustic> acoustics,
            final @NotNull List<Integer> weights)
    {
        final List<Acoustic> theAcoustics = new ObjectArrayList<>(acoustics);
        final float[] probabilityThresholds = new float[acoustics.size() - 1];

        float total = 0;
        for (int i = 0; i < weights.size(); i++) {
            if (weights.get(i) < 0) {
                throw new IllegalArgumentException("A probability weight can't be negative");
            }

            total = total + weights.get(i);
        }

        for (int i = 0; i < weights.size() - 1; i++) {
            probabilityThresholds[i] = weights.get(i) / total;
        }

        return new WeightedAcoustic(theAcoustics, probabilityThresholds);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull Acoustic fromJson(
            final @NotNull JsonObject json,
            final @NotNull AcousticsJsonParser context)
    {
        final List<Integer> weights = new ObjectArrayList<>();
        final List<Acoustic> acoustics = new ObjectArrayList<>();

        final JsonArray sim = json.getAsJsonArray("array");
        final Iterator<JsonElement> iter = sim.iterator();

        while (iter.hasNext()) {
            JsonElement subElement = iter.next();
            weights.add(subElement.getAsInt());

            if (!iter.hasNext()) {
                throw new JsonParseException("Probability has odd number of children!");
            }

            subElement = iter.next();
            acoustics.add(context.solveAcoustic(subElement));
        }

        return WeightedAcoustic.of(acoustics, weights);
    }

    public WeightedAcoustic {
        theAcoustics = new ObjectImmutableList<>(theAcoustics);
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        final float rand = player.getRNG().nextFloat();

        int marker = 0;
        while (marker < probabilityThresholds.length && probabilityThresholds[marker] < rand) {
            marker++;
        }

        theAcoustics.get(marker).playSound(player, location, event, inputOptions);
    }
}