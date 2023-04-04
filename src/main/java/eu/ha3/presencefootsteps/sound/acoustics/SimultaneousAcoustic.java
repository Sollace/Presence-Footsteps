package eu.ha3.presencefootsteps.sound.acoustics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * An acoustic that plays multiple other acoustics all at the same time.
 *
 * @author Hurry
 */
record SimultaneousAcoustic(@NotNull List<Acoustic> acoustics) implements Acoustic {

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull SimultaneousAcoustic of(
            @NotNull JsonArray sim,
            @NotNull AcousticsJsonParser context)
    {
        List<Acoustic> acoustics = new ObjectArrayList<>(sim.size());

        for (JsonElement i : sim) {
            acoustics.add(context.solveAcoustic(i));
        }

        return new SimultaneousAcoustic(acoustics);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull SimultaneousAcoustic fromJson(
            @NotNull JsonObject json,
            @NotNull AcousticsJsonParser context)
    {
        return of(json.getAsJsonArray("array"), context);
    }

    public SimultaneousAcoustic {
        acoustics = new ObjectImmutableList<>(acoustics);
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        acoustics.forEach(acoustic -> acoustic.playSound(player, location, event, inputOptions));
    }
}