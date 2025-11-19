package eu.ha3.presencefootsteps.sound.acoustics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import net.minecraft.world.entity.LivingEntity;

record ChanceAcoustic(
        Acoustic acoustic,
        float probability
) implements Acoustic {
    public static final MapCodec<ChanceAcoustic> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Acoustic.CODEC.fieldOf("acoustic").forGetter(ChanceAcoustic::acoustic),
            Codec.FLOAT.fieldOf("probability").forGetter(ChanceAcoustic::probability)
    ).apply(i, ChanceAcoustic::new));
    @SuppressWarnings("deprecation")
    @Deprecated
    static final Serializer FACTORY = Serializer.ofJsObject((json, context) -> new ChanceAcoustic(
        Acoustic.read(context, json.get("acoustic")),
        json.get("probability").getAsFloat()
    ));

    @Override
    public String type() {
        return Acoustic.CHANCE;
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        if (player.getRNG().nextFloat() * 100 <= probability) {
            acoustic.playSound(player, location, event, inputOptions);
        }
    }
}