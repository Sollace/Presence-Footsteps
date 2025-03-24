package eu.ha3.presencefootsteps.sound.acoustics;

import java.io.IOException;

import com.google.gson.JsonObject;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import eu.ha3.presencefootsteps.util.JsonObjectWriter;
import eu.ha3.presencefootsteps.util.Range;
import net.minecraft.entity.LivingEntity;

/**
 * The simplest form of an acoustic. Plays one sound with a set volume and pitch range.
 *
 * @author Hurry
 */
record VaryingAcoustic(
        String soundName,
        Range volume,
        Range pitch
) implements Acoustic {
    static final Serializer FACTORY = (json, context) -> {
        if (json.isJsonPrimitive()) {
            return new VaryingAcoustic(
                context.getSoundName(json.getAsString()),
                context.defaultVolume(),
                context.defaultPitch()
            );
        }
        JsonObject jso = json.getAsJsonObject();
        if (!jso.has("name")) {
            return EmptyAcoustic.INSTANCE;
        }
        String name = jso.get("name").getAsString();
        if (name.isEmpty()) {
            return EmptyAcoustic.INSTANCE;
        }
        return new VaryingAcoustic(
                context.getSoundName(name),
                context.defaultVolume().read("volume", jso),
                context.defaultPitch().read("pitch", jso)
        );
    };

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        if (soundName.isEmpty()) {
            // Special case for intentionally empty sounds (as opposed to fall back sounds)
            return;
        }

        final float finalVolume = (inputOptions.containsKey("gliding_volume")
                ? volume.on(inputOptions.get("gliding_volume"))
                : volume.random(player.getRNG())) * inputOptions.getOrDefault("volume_scale", 1F);

        final float finalPitch = inputOptions.containsKey("gliding_pitch")
                ? pitch.on(inputOptions.get("gliding_pitch"))
                : pitch.random(player.getRNG());

        player.playSound(location, soundName, finalVolume, finalPitch, inputOptions);
    }

    @Override
    public void write(AcousticsFile context, JsonObjectWriter writer) throws IOException {
        boolean hasVolume = !volume.equals(context.defaultVolume());
        boolean hasPitch = !pitch.equals(context.defaultPitch());
        if (hasVolume || hasPitch) {
            writer.object(() -> {
                if (soundName != null && !soundName.isEmpty()) {
                    writer.field("type", "basic");
                    writer.field("name", !context.soundRoot().isEmpty() && soundName.startsWith(context.soundRoot()) ? soundName.replace(context.soundRoot(), "") : "@" + soundName);
                    if (hasVolume) {
                        writer.field("volume", () -> volume.write(writer));
                    }
                    if (hasPitch) {
                        writer.field("pitch", () -> pitch.write(writer));
                    }
                }
            });
        } else {
            writer.writer().value(!context.soundRoot().isEmpty() && soundName.startsWith(context.soundRoot()) ? soundName.replace(context.soundRoot(), "") : "@" + soundName);
        }
    }
}