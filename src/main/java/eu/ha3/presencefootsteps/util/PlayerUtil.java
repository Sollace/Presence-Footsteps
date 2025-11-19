package eu.ha3.presencefootsteps.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public interface PlayerUtil {
    static boolean isClientPlayer(Entity entity) {
        Player client = Minecraft.getInstance().player;

        return entity instanceof Player
                && !(entity instanceof RemotePlayer)
                && client != null
                && (client == entity || client.getUUID().equals(entity.getUUID()));
    }

    static float getScale(LivingEntity entity) {
        return Mth.clamp(entity.getBbWidth() / entity.getType().getDimensions().width(), 0.01F, 200F);
    }
}
