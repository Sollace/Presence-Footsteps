package eu.ha3.presencefootsteps.sound.acoustics;

import com.google.gson.JsonObject;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import eu.ha3.presencefootsteps.util.Period;
import eu.ha3.presencefootsteps.util.Range;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

record DelayedAcoustic(
        @NotNull String soundName,
        @NotNull Range volume,
        @NotNull Range pitch,
        @NotNull Period delay
) implements Acoustic {

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull DelayedAcoustic of(
            final @NotNull String name,
            final @NotNull AcousticsJsonParser context)
    {
        final Range volume = new Range(1);
        final Range pitch = new Range(1);
        final Period delay = new Period(0);

        volume.copy(context.getVolumeRange());
        pitch.copy(context.getPitchRange());

        return new DelayedAcoustic(
                context.getSoundName(name),
                volume, pitch, delay
        );
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull DelayedAcoustic fromJson(
            final @NotNull JsonObject json,
            final @NotNull AcousticsJsonParser context)
    {
        final DelayedAcoustic acoustic = DelayedAcoustic.of(json.get("name").getAsString(), context);
        final Period delay = new Period(0);

        acoustic.volume.read("vol", json, context);
        acoustic.pitch.read("pitch", json, context);

        if (json.has("delay")) {
            delay.set(json.get("delay").getAsLong());
        } else {
            delay.set(json.get("delay_min").getAsLong(), json.get("delay_max").getAsLong());
        }

        return new DelayedAcoustic(
                acoustic.soundName,
                acoustic.volume,
                acoustic.pitch,
                delay
        );
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        if (soundName.isEmpty()) {
            // Special case for intentionally empty sounds (as opposed to fall back sounds)
            return;
        }

        final float volume = inputOptions.containsKey("gliding_volume")
                ? this.volume.on(inputOptions.get("gliding_volume"))
                : this.volume.random(player.getRNG());

        final float pitch = inputOptions.containsKey("gliding_pitch")
                ? this.pitch.on(inputOptions.get("gliding_pitch"))
                : this.pitch.random(player.getRNG());

        player.playSound(location, this.soundName, volume, pitch, this.delay);
    }

}