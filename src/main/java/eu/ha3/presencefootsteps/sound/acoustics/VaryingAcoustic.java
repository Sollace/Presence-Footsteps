package eu.ha3.presencefootsteps.sound.acoustics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import eu.ha3.presencefootsteps.util.Range;
import net.minecraft.world.entity.LivingEntity;

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
    public static final MapCodec<VaryingAcoustic> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(VaryingAcoustic::soundName),
            Range.CODEC.fieldOf("volume").forGetter(VaryingAcoustic::volume),
            Range.CODEC.fieldOf("pitch").forGetter(VaryingAcoustic::pitch)
    ).apply(i, VaryingAcoustic::new));
    @SuppressWarnings("deprecation")
    @Deprecated
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
            throw new JsonParseException("Acoustic is missing a name");
        }
        String name = jso.get("name").getAsString();
        if (name.isEmpty()) {
            throw new JsonParseException("Acoustic is missing a name");
        }
        return new VaryingAcoustic(
                context.getSoundName(name),
                context.defaultVolume().read("volume", jso),
                context.defaultPitch().read("pitch", jso)
        );
    };

    @Override
    public String type() {
        return Acoustic.BASIC;
    }

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
}