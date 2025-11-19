package eu.ha3.presencefootsteps.config;

import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public enum EntitySelector implements Predicate<Entity> {
    ALL {
        @Override
        public boolean test(Entity e) {
            return true;
        }
    },
    PLAYERS_AND_HOSTILES {
        @Override
        public boolean test(Entity e) {
            return e instanceof Player || e instanceof Enemy;
        }
    },
    PLAYERS_ONLY {
        @Override
        public boolean test(Entity e) {
            return e instanceof Player;
        }
    };

    public static final EntitySelector[] VALUES = values();
}
