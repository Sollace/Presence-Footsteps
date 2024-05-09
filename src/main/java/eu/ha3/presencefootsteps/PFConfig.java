package eu.ha3.presencefootsteps;

import java.nio.file.Path;

import eu.ha3.presencefootsteps.config.EntitySelector;
import eu.ha3.presencefootsteps.config.JsonFile;
import eu.ha3.presencefootsteps.config.VolumeOption;
import eu.ha3.presencefootsteps.sound.generator.Locomotion;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;

public class PFConfig extends JsonFile {

    private int volume = 70;

    public VolumeOption clientPlayerVolume = new VolumeOption(this, 100);
    public VolumeOption otherPlayerVolume = new VolumeOption(this, 100);
    public VolumeOption hostileEntitiesVolume = new VolumeOption(this, 100);
    public VolumeOption passiveEntitiesVolume = new VolumeOption(this, 100);
    public int runningVolumeIncrease = 0;
    public VolumeOption wetSoundsVolume = new VolumeOption(this, 50);
    public VolumeOption foliageSoundsVolume = new VolumeOption(this, 100);

    private int maxSteppingEntities = 50;

    private boolean disabled = false;
    private boolean firstRun = true;
    private boolean multiplayer = true;
    private boolean global = true;
    private boolean footwear = true;
    private boolean visualiser = false;
    private boolean exclusive = false;

    private Locomotion stance = Locomotion.NONE;
    private EntitySelector targetEntities = EntitySelector.ALL;

    private transient final PresenceFootsteps pf;

    public PFConfig(Path file, PresenceFootsteps pf) {
        super(file);
        this.pf = pf;
    }

    public boolean toggleMultiplayer() {
        multiplayer = !multiplayer;
        save();

        return multiplayer;
    }

    public EntitySelector cycleTargetSelector() {
        targetEntities = EntitySelector.VALUES[(getEntitySelector().ordinal() + 1) % EntitySelector.VALUES.length];

        save();

        return targetEntities;
    }

    public Locomotion setLocomotion(Locomotion loco) {
        if (loco != getLocomotion()) {
            stance = loco;
            save();

            pf.getEngine().reload();
        }

        return loco;
    }

    public boolean isVisualiserRunning() {
        return visualiser;
    }

    public boolean isFirstRun() {
        return firstRun;
    }

    public void setNotFirstRun() {
        firstRun = false;
        save();
    }

    public Locomotion getLocomotion() {
        return stance == null ? Locomotion.NONE : stance;
    }

    public EntitySelector getEntitySelector() {
        return targetEntities == null ? EntitySelector.ALL : targetEntities;
    }

    public boolean getEnabledFootwear() {
        return footwear;
    }

    public boolean toggleFootwear() {
        footwear = !footwear;
        save();
        return footwear;
    }

    public boolean isExclusiveMode() {
        return exclusive;
    }

    public boolean toggleExclusiveMode() {
        exclusive = !exclusive;
        save();
        return exclusive;
    }

    public boolean getEnabledMP() {
        return multiplayer;
    }

    public int getMaxSteppingEntities() {
        return Math.max(1, maxSteppingEntities);
    }

    public boolean toggleDisabled() {
        disabled = !disabled;
        save();
        pf.onEnabledStateChange(!disabled);
        return disabled;
    }

    public boolean setDisabled(boolean disabled) {
        if (disabled != this.disabled) {
            toggleDisabled();
        }
        return disabled;
    }

    public boolean getDisabled() {
        return disabled;
    }

    public boolean getEnabled() {
        return !disabled && getGlobalVolume() > 0;
    }

    public int getGlobalVolume() {
        return MathHelper.clamp(volume, 0, 100);
    }

    public int getRunningVolumeIncrease() {
        return MathHelper.clamp(runningVolumeIncrease, -100, 100);
    }

    public float setGlobalVolume(float volume) {
        volume = volumeScaleToInt(volume);

        if (this.volume != volume) {
            boolean wasEnabled = getEnabled();

            this.volume = (int)volume;
            save();

            if (getEnabled() != wasEnabled) {
                pf.onEnabledStateChange(getEnabled());
            }
        }

        return getGlobalVolume();
    }

    public float setRunningVolumeIncrease(float volume) {
        runningVolumeIncrease = volume > 97 ? 100 : volume < -97 ? -100 : (int)volume;
        save();
        return getRunningVolumeIncrease();
    }

    public void populateCrashReport(CrashReportSection section) {
        section.add("Disabled", getDisabled());
        section.add("Global Volume", volume);
        section.add("User's Selected Stance", getLocomotion());
        section.add("Target Selector", getEntitySelector());
        section.add("Enabled Global", global);
        section.add("Enabled Multiplayer", multiplayer);
    }

    private static int volumeScaleToInt(float volume) {
        return volume > 97 ? 100 : volume < 3 ? 0 : (int)volume;
    }
}
