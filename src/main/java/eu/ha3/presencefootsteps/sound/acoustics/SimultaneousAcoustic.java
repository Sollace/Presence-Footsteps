package eu.ha3.presencefootsteps.sound.acoustics;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * An acoustic that plays multiple other acoustics all at the same time.
 *
 * @author Hurry
 */
record SimultaneousAcoustic(List<Acoustic> acoustics) implements Acoustic {
    public static final Codec<SimultaneousAcoustic> CODEC = Acoustic.CODEC.listOf().xmap(SimultaneousAcoustic::new, SimultaneousAcoustic::acoustics);
    public static final MapCodec<SimultaneousAcoustic> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Acoustic.CODEC.listOf().fieldOf("acoustics").forGetter(SimultaneousAcoustic::acoustics)
    ).apply(i, SimultaneousAcoustic::new));

    @SuppressWarnings("deprecation")
    @Deprecated
    static final Serializer FACTORY = (json, context) -> new SimultaneousAcoustic(
            (json.isJsonArray() ? json.getAsJsonArray() : json.getAsJsonObject().getAsJsonArray("array")).asList()
            .stream()
            .map(i -> Acoustic.read(context, i))
            .toList()
    );

    @Override
    public String type() {
        return Acoustic.SIMULTANEOUS;
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        acoustics.forEach(acoustic -> acoustic.playSound(player, location, event, inputOptions));
    }

    @Override
    public Stream<Entry<String, String>> sounds() {
        final int[] index = new int[1];
        return acoustics.stream().flatMap(a -> {
            int i = index[0]++;
            return a.sounds().map(e -> Map.entry("simultaneous[" + i + "]." + e.getKey(), e.getValue()));
        });
    }
}