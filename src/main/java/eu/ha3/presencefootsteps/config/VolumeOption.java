package eu.ha3.presencefootsteps.config;

import com.minelittlepony.common.client.gui.IField.IChangeCallback;
import com.minelittlepony.common.util.settings.Config;
import com.minelittlepony.common.util.settings.Setting;

import net.minecraft.util.Mth;

public class VolumeOption implements IChangeCallback<Float> {

    private final Config config;
    private final Setting<Integer> value;

    public VolumeOption(Config config, Setting<Integer> value) {
        this.config = config;
        this.value = value;
    }

    public int get() {
        return Mth.clamp(value.get(), 0, 100);
    }

    public float getPercentage() {
        return get() / 100F;
    }

    public float set(float volume) {
        value.set(volumeScaleToInt(volume));
        config.save();
        return get();
    }

    @Override
    public Float perform(Float value) {
        return set(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    private static int volumeScaleToInt(float volume) {
        return volume > 97 ? 100 : volume < 3 ? 0 : (int)volume;
    }
}
