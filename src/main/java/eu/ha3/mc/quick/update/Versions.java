package eu.ha3.mc.quick.update;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.util.GsonHelper;
import java.util.List;

public record Versions (
        TargettedVersion latest,
        List<TargettedVersion> previous) {

    public Versions(JsonObject json) throws VersionParsingException {
        this(new TargettedVersion(GsonHelper.getAsJsonObject(json, "latest")), new ObjectArrayList<>());
        for (var el : GsonHelper.getAsJsonArray(json, "previous")) {
            previous.add(new TargettedVersion(el.getAsJsonObject()));
        }
    }
}
