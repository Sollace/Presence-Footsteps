package eu.ha3.presencefootsteps.util;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.function.Consumer;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public interface ResourceUtils {
    static boolean forEach(Identifier id, ResourceManager manager, Consumer<Reader> consumer) {
        return manager.getAllResources(id).stream().mapToInt(res -> {
            try (Reader stream = new InputStreamReader(res.getInputStream())) {
                consumer.accept(stream);
                return 1;
            } catch (Exception e) {
                PresenceFootsteps.logger.error("Error encountered loading resource " + id + " from pack" + res.getResourcePackName(), e);
                return 0;
            }
        }).sum() > 0;
    }

    static boolean forEachReverse(Identifier id, ResourceManager manager, Consumer<Reader> consumer) {
        List<Resource> resources = manager.getAllResources(id);
        for (int i = resources.size() - 1; i >= 0; i--) {
            Resource res = resources.get(i);
            try (Reader stream = new InputStreamReader(res.getInputStream())) {
                consumer.accept(stream);
            } catch (Exception e) {
                PresenceFootsteps.logger.error("Error encountered loading resource " + id + " from pack" + res.getResourcePackName(), e);
            }
        }
        return !resources.isEmpty();
    }
}
