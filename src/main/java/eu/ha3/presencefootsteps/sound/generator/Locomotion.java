package eu.ha3.presencefootsteps.sound.generator;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public enum Locomotion {
    NONE,
    BIPED((entity, engine) -> new TerrestrialStepSoundGenerator(entity, engine, new Modifier<>())),
    QUADRUPED((entity, engine) -> new TerrestrialStepSoundGenerator(entity, engine, new QuadrupedModifier())),
    FLYING((entity, engine) -> new WingedStepSoundGenerator(entity, engine, new QuadrupedModifier())),
    FLYING_BIPED((entity, engine) -> new WingedStepSoundGenerator(entity, engine, new Modifier<>()));

    private static final Map<String, Locomotion> registry = new Object2ObjectOpenHashMap<>();

    static {
        for (Locomotion i : values()) {
            registry.put(i.name(), i);
            registry.put(String.valueOf(i.ordinal()), i);
        }
    }

    private final BiFunction<LivingEntity, SoundEngine, Optional<StepSoundGenerator>> constructor;

    private static final String AUTO_TRANSLATION_KEY = "menu.pf.stance.auto";
    private final String translationKey = "menu.pf.stance." + name().toLowerCase(Locale.ROOT);

    Locomotion() {
        constructor = (entity, engine) -> Optional.empty();
    }

    Locomotion(BiFunction<LivingEntity, SoundEngine, StepSoundGenerator> gen) {
        constructor = (entity, engine) -> Optional.of(gen.apply(entity, engine));
    }

    public Optional<StepSoundGenerator> supplyGenerator(LivingEntity entity, SoundEngine engine) {
        return constructor.apply(entity, engine);
    }

    public Component getOptionName() {
        return Component.translatable("menu.pf.stance", Component.translatable(this == NONE ? AUTO_TRANSLATION_KEY : translationKey));
    }

    public Component getOptionTooltip() {
        return Component.translatable(translationKey + ".tooltip");
    }

    public static Locomotion byName(String name) {
        return registry.getOrDefault(name, BIPED);
    }

    public static Locomotion forLiving(Entity entity, Locomotion fallback) {
        if (MineLP.hasPonies()) {
            return MineLP.getLocomotion(entity, fallback);
        }

        return fallback;
    }

    public static Locomotion forPlayer(Player ply, Locomotion preference) {
        if (preference == NONE) {
            if (ply instanceof LocalPlayer && MineLP.hasPonies()) {
                return MineLP.getLocomotion(ply);
            }

            return Locomotion.BIPED;
        }

        return preference;
    }
}
