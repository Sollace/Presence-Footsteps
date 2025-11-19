package eu.ha3.presencefootsteps.world;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonObject;

import eu.ha3.presencefootsteps.util.JsonObjectWriter;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

public class PrimitiveLookup extends AbstractSubstrateLookup<SoundEvent> {
    public PrimitiveLookup(JsonObject json) {
        super(json);
    }

    @Override
    protected Identifier getId(SoundEvent key) {
        return key.location();
    }

    public static void writeToReport(Lookup<SoundEvent> lookup, boolean full, JsonObjectWriter writer, Map<String, SoundType> groups) throws IOException {
        writer.each(groups.values(), group -> {
            SoundEvent event = group.getStepSound();
            if (event != null && (full || !lookup.contains(event))) {
                writer.field(getKey(group), lookup.getAssociation(event, getSubstrate(group)).raw());
            }
        });
    }

    public static String getSubstrate(SoundType group) {
        return String.format(Locale.ENGLISH, "%.2f_%.2f", group.volume, group.pitch);
    }

    public static String getKey(SoundType group) {
        return group.getStepSound().location().toString() + "@" + getSubstrate(group);
    }
}
