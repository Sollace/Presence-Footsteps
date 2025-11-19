package eu.ha3.mc.quick.update;

import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.util.GsonHelper;

public record TargettedVersion (
        Version minecraft,
        Version version) {

    public TargettedVersion(JsonObject json) throws VersionParsingException {
        this(
                Version.parse(GsonHelper.getAsString(json, "minecraft")),
                Version.parse(GsonHelper.getAsString(json, "version")));
    }

    public TargettedVersion(String modid) {
        this(getVersion("minecraft"), getVersion(modid));
    }

    public static Version getVersion(String modid) {
        return FabricLoader.getInstance().getModContainer(modid)
                .map(ModContainer::getMetadata)
                .map(ModMetadata::getVersion)
                .orElseThrow(() -> new IllegalArgumentException("Unknown mod id: " + modid));
    }
}