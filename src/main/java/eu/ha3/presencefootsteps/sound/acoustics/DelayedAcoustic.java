package eu.ha3.presencefootsteps.sound.acoustics;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import eu.ha3.presencefootsteps.util.Period;
import net.minecraft.world.entity.LivingEntity;

record DelayedAcoustic(
        Acoustic acoustic,
        Period delay
) implements Acoustic {
    public static final MapCodec<DelayedAcoustic> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Acoustic.CODEC.fieldOf("acoustic").forGetter(DelayedAcoustic::acoustic),
            Period.CODEC.fieldOf("delay").forGetter(DelayedAcoustic::delay)
    ).apply(i, DelayedAcoustic::new));

    @SuppressWarnings("deprecation")
    @Deprecated
    static final Serializer FACTORY = Serializer.ofJsObject((json, context) -> new DelayedAcoustic(
        json.has("name") ? VaryingAcoustic.FACTORY.create(json, context) : Acoustic.read(context, json.get("acoustic")),
        Period.fromJson(json, "delay")
    ));

    @Override
    public String type() {
        return Acoustic.DELAYED;
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        acoustic.playSound(player, location, event, inputOptions.and(delay));
    }
}