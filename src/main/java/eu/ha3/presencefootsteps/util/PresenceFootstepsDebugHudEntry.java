package eu.ha3.presencefootsteps.util;
import eu.ha3.presencefootsteps.PresenceFootsteps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntry;
import net.minecraft.client.gui.hud.debug.DebugHudLines;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PresenceFootstepsDebugHudEntry implements DebugHudEntry {
    @Override
    public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Entity entity = minecraftClient.getCameraEntity();
        if (entity != null && world != null) {
            HitResult hitResult = entity.raycast(20.0, 0.0F, false);
            PresenceFootsteps.getInstance().getDebugHud().render(hitResult, hitResult, lines);
        }
    }

    @Override
    public boolean canShow(boolean reducedDebugInfo) {
        return true;
    }
}

