package eu.ha3.presencefootsteps.sound.acoustics;

import com.google.gson.JsonObject;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.LivingEntity;

import java.util.Map;

/**
 * An acoustic that can play different acoustics depending on a specific event type.
 *
 * @author Hurry
 */
class EventSelectorAcoustics implements Acoustic {
    private final Map<State, Acoustic> pairs = new Object2ObjectOpenHashMap<>();

    public EventSelectorAcoustics(JsonObject json, AcousticsJsonParser context) {
        for (State i : State.values()) {
            String eventName = i.getName();

            if (json.has(eventName)) {
                pairs.put(i, context.solveAcoustic(json.get(eventName)));
            }
        }
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        if (pairs.containsKey(event)) {
            pairs.get(event).playSound(player, location, event, inputOptions);
        } else if (event.canTransition()) {
            playSound(player, location, event.getTransitionDestination(), inputOptions);
            // the possibility of a resonance cascade scenario is extremely unlikely
        }
    }
}