package eu.ha3.mc.quick.update;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.util.JsonHelper;

import java.util.List;

public record Versions (
        TargettedVersion latest,
        List<TargettedVersion> previous) {

    public Versions(JsonObject json) throws VersionParsingException {
        this(new TargettedVersion(JsonHelper.getObject(json, "latest")), new ObjectArrayList<>());
        for (var el : JsonHelper.getArray(json, "previous")) {
            previous.add(new TargettedVersion(el.getAsJsonObject()));
        }
    }
}
