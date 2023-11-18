package eu.ha3.presencefootsteps.sound.generator;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.api.pony.meta.Race;
import com.minelittlepony.client.MineLittlePony;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class MineLP {
    private static boolean checkCompleted = false;
    private static boolean hasMineLP;

    public static boolean hasPonies() {
        if (!checkCompleted) {
            checkCompleted = true;
            hasMineLP = FabricLoader.getInstance().isModLoaded("minelp");
        }

        return hasMineLP;
    }

    public static Locomotion getLocomotion(Entity entity, Locomotion fallback) {

        @Nullable
        Identifier texture = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity).getTexture(entity);

        if (texture == null) {
            // Bad modder. /slap
            return fallback;
        }

        Race race = MineLittlePony.getInstance().getManager().getPony(texture).race();

        if (race.isHuman()) {
            return fallback;
        }

        return race.hasWings() ? Locomotion.FLYING : Locomotion.QUADRUPED;
    }

    public static Locomotion getLocomotion(PlayerEntity ply) {
        return MineLittlePony.getInstance().getManager().getPony(ply).race().hasWings()
                ? Locomotion.FLYING
                : Locomotion.QUADRUPED;
    }
}
