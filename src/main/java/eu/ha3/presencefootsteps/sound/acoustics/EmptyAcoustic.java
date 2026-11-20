package eu.ha3.presencefootsteps.sound.acoustics;

import java.util.Map.Entry;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * An empty acoustic that does nothing. This is used instead of an empty name or simply "@" in legacy files
 *
 * @author Sollace
 */
record EmptyAcoustic() implements Acoustic {
    public static final EmptyAcoustic INSTANCE = new EmptyAcoustic();
    public static final MapCodec<EmptyAcoustic> CODEC = MapCodec.unit(INSTANCE);
    @SuppressWarnings("deprecation")
    @Deprecated
    static final Serializer FACTORY = (_, _) -> INSTANCE;

    @Override
    public String type() {
        return Acoustic.EMPTY;
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {

    }

    @Override
    public Stream<Entry<String, String>> sounds() {
        return Stream.empty();
    }
}