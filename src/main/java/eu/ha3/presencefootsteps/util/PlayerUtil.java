package eu.ha3.presencefootsteps.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class PlayerUtil {
    public static boolean isClientPlayer(Entity entity) {
        PlayerEntity client = MinecraftClient.getInstance().player;

        return entity instanceof PlayerEntity
                && !(entity instanceof OtherClientPlayerEntity)
                && client != null
                && (client == entity || client.getUuid().equals(entity.getUuid()));
    }

    public static float getScale(LivingEntity entity) {
        return MathHelper.clamp(entity.getWidth() / entity.getType().getDimensions().width, 0.01F, 200F);
    }
}
