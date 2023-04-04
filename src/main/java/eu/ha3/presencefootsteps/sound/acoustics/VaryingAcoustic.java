package eu.ha3.presencefootsteps.sound.acoustics;

import com.google.gson.JsonObject;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import eu.ha3.presencefootsteps.util.Range;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The simplest form of an acoustic. Plays one sound with a set volume and pitch range.
 *
 * @author Hurry
 */
record VaryingAcoustic(
        @NotNull String soundName,
        @NotNull Range volume,
        @NotNull Range pitch
) implements Acoustic {

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull VaryingAcoustic of(
            final @NotNull String name,
            final @NotNull AcousticsJsonParser context)
    {
        final Range volume = new Range(1);
        final Range pitch = new Range(1);

        volume.copy(context.getVolumeRange());
        pitch.copy(context.getPitchRange());

        return new VaryingAcoustic(
                context.getSoundName(name),
                volume, pitch
        );
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull VaryingAcoustic fromJson(
            final @NotNull JsonObject json,
            final @NotNull AcousticsJsonParser context)
    {
        final VaryingAcoustic acoustic = VaryingAcoustic.of(json.get("name").getAsString(), context);

        acoustic.volume.read("vol", json, context);
        acoustic.pitch.read("pitch", json, context);

        return new VaryingAcoustic(
                acoustic.soundName,
                acoustic.volume,
                acoustic.pitch
        );
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        playSound(this.soundName, this.volume, this.pitch, Options.EMPTY, player, location, inputOptions);
    }

    // shared code between VaryingAcoustic & DelayedAcoustic since
    // in the old implementation DelayedAcoustic extended VaryingAcoustic
    @ApiStatus.Internal
    static void playSound(@NotNull String soundName, Range volume, Range pitch, Options delay,
                          SoundPlayer player, LivingEntity location, Options inputOptions)
    {
        if (soundName.isEmpty()) {
            // Special case for intentionally empty sounds (as opposed to fall back sounds)
            return;
        }

        final float finalVolume = inputOptions.containsKey("gliding_volume")
                ? volume.on(inputOptions.get("gliding_volume"))
                : volume.random(player.getRNG());

        final float finalPitch = inputOptions.containsKey("gliding_pitch")
                ? pitch.on(inputOptions.get("gliding_pitch"))
                : pitch.random(player.getRNG());

        player.playSound(location, soundName, finalVolume, finalPitch, delay);
    }
}