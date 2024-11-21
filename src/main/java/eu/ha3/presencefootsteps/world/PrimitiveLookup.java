package eu.ha3.presencefootsteps.world;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import eu.ha3.presencefootsteps.util.JsonObjectWriter;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class PrimitiveLookup extends AbstractSubstrateLookup<SoundEvent> {
    @Override
    protected Identifier getId(SoundEvent key) {
        return key.id();
    }

    @Override
    public void writeToReport(boolean full, JsonObjectWriter writer, Map<String, BlockSoundGroup> groups) throws IOException {
        writer.each(groups.values(), group -> {
            SoundEvent event = group.getStepSound();
            if (event != null && (full || !contains(event))) {
                writer.field(getKey(group), getAssociation(event, getSubstrate(group)).raw());
            }
        });
    }

    public static String getSubstrate(BlockSoundGroup group) {
        return String.format(Locale.ENGLISH, "%.2f_%.2f", group.volume, group.pitch);
    }

    public static String getKey(BlockSoundGroup group) {
        return group.getStepSound().id().toString() + "@" + getSubstrate(group);
    }
}
