package eu.ha3.presencefootsteps.world;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;

import eu.ha3.presencefootsteps.util.JsonObjectWriter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.SoundType;

public class GolemLookup extends AbstractSubstrateLookup<EntityType<?>> {
    public GolemLookup(JsonObject json) {
        super(json);
    }

    @Override
    public Optional<SoundsKey> getAssociation(EntityType<?> key, String substrate) {
        return getSubstrateMap(getId(key), substrate).getOrDefault(getId(key), Optional.empty());
    }

    @Override
    protected Identifier getId(EntityType<?> key) {
        return EntityType.getKey(key);
    }

    public static void writeToReport(Lookup<EntityType<?>> lookup, boolean full, JsonObjectWriter writer, Map<String, SoundType> groups) throws IOException {
        writer.each(BuiltInRegistries.ENTITY_TYPE, type -> {
            if (full || !lookup.contains(type)) {
                writer.object(EntityType.getKey(type).toString(), () -> {
                    writer.object("associations", () -> {
                        lookup.getSubstrates().forEach(substrate -> {
                            try {
                                SoundsKey association = lookup.getAssociation(type, substrate);
                                if (association.isResult()) {
                                    writer.field(substrate, association.raw());
                                }
                            } catch (IOException ignore) {}
                        });
                    });
                });
            }
        });
    }
}
