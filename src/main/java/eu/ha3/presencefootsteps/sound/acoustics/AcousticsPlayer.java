package eu.ha3.presencefootsteps.sound.acoustics;

import java.util.*;

import com.google.common.base.Strings;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.State;
import eu.ha3.presencefootsteps.sound.Options;
import eu.ha3.presencefootsteps.sound.player.SoundPlayer;
import eu.ha3.presencefootsteps.world.Emitter;
import net.minecraft.entity.LivingEntity;

public class AcousticsPlayer implements AcousticLibrary {

    private final SoundPlayer player;

    private final Map<String, Acoustic> acoustics = new HashMap<>();

    public AcousticsPlayer(SoundPlayer player) {
        this.player = player;
    }

    @Override
    public void addAcoustic(String name, Acoustic acoustic) {
        acoustics.put(name, acoustic);
    }

    @Override
    public void playAcoustic(LivingEntity location, String acousticName, State event, Options inputOptions) {
        if (!Emitter.isEmitter(acousticName)) {
            return;
        }

        if (acousticName.contains(",")) {
            List.of(acousticName.split(","))
                .stream()
                .map(Strings::nullToEmpty)
                .filter(s -> !s.isEmpty())
                .distinct()
                .forEach(fragment -> playAcoustic(location, fragment, event, inputOptions));
        } else if (!acoustics.containsKey(acousticName)) {
            PresenceFootsteps.logger.warn("Tried to play a missing acoustic: " + acousticName);
        } else {
            acoustics.get(acousticName).playSound(player, location, event, inputOptions);
        }
    }
}
