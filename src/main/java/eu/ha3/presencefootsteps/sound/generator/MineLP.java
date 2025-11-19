package eu.ha3.presencefootsteps.sound.generator;

import com.minelittlepony.api.pony.Pony;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

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
        return Pony.getManager().getPony(entity)
                .map(Pony::race)
                .filter(race -> !race.isHuman())
                .map(race -> race.hasWings() ? Locomotion.FLYING : Locomotion.QUADRUPED)
                .orElse(fallback);
    }

    public static Locomotion getLocomotion(Player ply) {
        return getLocomotion(ply, Locomotion.BIPED);
    }
}
