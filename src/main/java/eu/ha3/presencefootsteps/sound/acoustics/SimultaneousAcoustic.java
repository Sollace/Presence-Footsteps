package eu.ha3.presencefootsteps.sound.acoustics;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.LivingEntity;

/**
 * An acoustic that plays multiple other acoustics all a the same time.
 *
 * @author Hurry
 */
class SimultaneousAcoustic implements Acoustic {

    private final List<Acoustic> acoustics = new ObjectArrayList<>();

    public SimultaneousAcoustic(JsonObject json, AcousticsJsonParser context) {
        this(json.getAsJsonArray("array"), context);
    }

    public SimultaneousAcoustic(JsonArray sim, AcousticsJsonParser context) {
        for (JsonElement i : sim) {
            acoustics.add(context.solveAcoustic(i));
        }
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        acoustics.forEach(acoustic -> acoustic.playSound(player, location, event, inputOptions));
    }
}
