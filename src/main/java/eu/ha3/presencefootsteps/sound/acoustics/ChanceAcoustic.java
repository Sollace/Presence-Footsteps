package eu.ha3.presencefootsteps.sound.acoustics;

import com.google.gson.JsonObject;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

record ChanceAcoustic(
        @NotNull Acoustic acoustic,
        float probability
) implements Acoustic {

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull Acoustic fromJson(
            final @NotNull JsonObject json,
            final @NotNull AcousticsJsonParser context)
    {
        final Acoustic acoustic = context.solveAcoustic(json.get("acoustic"));
        final float probability = json.get("probability").getAsFloat();

        return new ChanceAcoustic(acoustic, probability);
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        final float rand = player.getRNG().nextFloat();

        if (rand * 100 <= probability) {
            acoustic.playSound(player, location, event, inputOptions);
        }
    }
}