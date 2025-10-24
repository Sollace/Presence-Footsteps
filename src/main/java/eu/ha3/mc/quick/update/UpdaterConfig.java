package eu.ha3.mc.quick.update;

import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;

import eu.ha3.presencefootsteps.config.JsonFile;

public class UpdaterConfig extends JsonFile {
    /**
     * Whether to check for updates
     */
    public boolean enabled;

    /**
     * The number of times to notify the user of a new update
     * <p>
     * The displayRemaining is set to this value when a new version is found.
     */
    public int displayCount;

    /**
     * The number of times remaining until the update notifier stops notifying users of a new version.
     */
    public int displayRemaining;

    /**
     * The last version to be displayed to the user.
     * <p>
     * The updater resets any time this changes
     */
    @Nullable
    public String lastKnownVersion;

    public UpdaterConfig(Path file) {
        super(file);
    }

    public boolean shouldReport(TargettedVersion newVersion) {
        try {
            String sVer = newVersion.toString();

            if (!sVer.equals(lastKnownVersion)) {
                lastKnownVersion = sVer;
                displayRemaining = displayCount;
            }

            return displayRemaining > 0 && displayRemaining-- >= 0;
        } finally {
            save();
        }
    }
}
