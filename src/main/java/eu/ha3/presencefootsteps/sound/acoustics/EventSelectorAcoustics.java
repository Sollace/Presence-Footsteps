package eu.ha3.presencefootsteps.sound.acoustics;

import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.StringIdentifiable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.KeyCompressor;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapLike;

/**
 * An acoustic that can play different acoustics depending on a specific event type.
 *
 * @author Hurry
 */
record EventSelectorAcoustics(Map<State, Acoustic> pairs) implements Acoustic {
    private static final MapCodec<EventSelectorAcoustics> MAP_CODEC = Codec.simpleMap(State.CODEC, Acoustic.CODEC, StringIdentifiable.toKeyable(State.values()))
            .xmap(EventSelectorAcoustics::new, EventSelectorAcoustics::pairs);
    public static final MapCodec<EventSelectorAcoustics> CODEC = MapCodec.of(MAP_CODEC, new MapDecoder<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return MAP_CODEC.keys(ops);
        }

        @Override
        public <T> DataResult<EventSelectorAcoustics> decode(DynamicOps<T> ops, MapLike<T> input) {
            input = MapLike.forMap(input.entries().filter(entry -> !entry.getFirst().equals(ops.createString("type"))).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)), ops);
            return MAP_CODEC.decode(ops, input);
        }

        @Override
        public <T> KeyCompressor<T> compressor(DynamicOps<T> ops) {
            return MAP_CODEC.compressor(ops);
        }

        @Override
        public String toString() {
            return MAP_CODEC.toString();
        }
    });

    @SuppressWarnings("deprecation")
    @Deprecated
    static final Serializer FACTORY = Serializer.ofJsObject((json, context) -> new EventSelectorAcoustics(Arrays.stream(State.values())
        .filter(i -> json.has(i.getName()))
        .collect(Collectors.toMap(
                Function.identity(),
                i -> Acoustic.read(context, json.get(i.getName()))
        ))));

    @Override
    public String type() {
        return Acoustic.EVENTS;
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