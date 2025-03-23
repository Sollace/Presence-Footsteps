package eu.ha3.presencefootsteps.world;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;

import eu.ha3.presencefootsteps.util.JsonObjectWriter;

public class GolemLookup extends AbstractSubstrateLookup<EntityType<?>> {
    public GolemLookup(JsonObject json) {
        super(json);
    }

    @Override
    public Optional<SoundsKey> getAssociation(EntityType<?> key, String substrate) {
        return getSubstrateMap(getId(key), substrate).getOrDefault(EntityType.getId(key), Optional.empty());
    }

    @Override
    protected Identifier getId(EntityType<?> key) {
        return EntityType.getId(key);
    }

    public static void writeToReport(Lookup<EntityType<?>> lookup, boolean full, JsonObjectWriter writer, Map<String, BlockSoundGroup> groups) throws IOException {
        writer.each(Registries.ENTITY_TYPE, type -> {
            if (full || !lookup.contains(type)) {
                writer.object(EntityType.getId(type).toString(), () -> {
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
