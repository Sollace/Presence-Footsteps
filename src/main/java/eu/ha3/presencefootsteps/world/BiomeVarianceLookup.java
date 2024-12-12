package eu.ha3.presencefootsteps.world;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import eu.ha3.presencefootsteps.util.JsonObjectWriter;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class BiomeVarianceLookup implements Index<Identifier, BiomeVarianceLookup.BiomeVariance> {
    private final Map<Identifier, BiomeVariance> entries = new HashMap<>();

    @Override
    public BiomeVariance lookup(Identifier key) {
        return entries.getOrDefault(key, BiomeVariance.DEFAULT);
    }

    @Override
    public boolean contains(Identifier key) {
        return entries.containsKey(key);
    }

    @Override
    public void add(String key, JsonElement value) {
        BiomeVariance.CODEC.decode(JsonOps.INSTANCE, value).result().map(Pair::getFirst).ifPresent(i -> {
            entries.put(Identifier.of(key), i);
        });
    }

    @Override
    public void writeToReport(boolean full, JsonObjectWriter writer, Map<String, BlockSoundGroup> groups) throws IOException {
    }

    public record BiomeVariance(float volume, float pitch) {
        public static final BiomeVariance DEFAULT = new BiomeVariance(1, 1);
        static final Codec<BiomeVariance> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("volume").forGetter(BiomeVariance::volume),
                Codec.FLOAT.fieldOf("pitch").forGetter(BiomeVariance::pitch)
        ).apply(i, BiomeVariance::new));
    }
}
