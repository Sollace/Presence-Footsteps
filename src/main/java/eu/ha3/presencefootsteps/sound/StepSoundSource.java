package eu.ha3.presencefootsteps.sound;

import java.util.Optional;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.generator.Locomotion;
import eu.ha3.presencefootsteps.sound.generator.StepSoundGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public interface StepSoundSource {
    Optional<StepSoundGenerator> getStepGenerator(SoundEngine engine);

    boolean isStepBlocked();

    final class Container implements StepSoundSource {
        private Locomotion locomotion;
        private Optional<StepSoundGenerator> stepSoundGenerator;

        private final LivingEntity entity;

        public Container(LivingEntity entity) {
            this.entity = entity;
        }

        @Override
        public Optional<StepSoundGenerator> getStepGenerator(SoundEngine engine) {
            Locomotion loco = engine.getIsolator().locomotions().lookup(entity);

            if (stepSoundGenerator == null || loco != locomotion) {
                locomotion = loco;
                stepSoundGenerator = loco.supplyGenerator(entity, engine);
            }
            return stepSoundGenerator;
        }

        @Override
        public boolean isStepBlocked() {
            SoundEngine engine = PresenceFootsteps.getInstance().getEngine();
            if (!Minecraft.getInstance().isSingleplayer() && Minecraft.getInstance().hasSingleplayerServer()) {
                return true;// Allow footsteps when in lan and multiplayer
            }
            if (!engine.getConfig().isExclusiveMode() && !(entity instanceof Player)) {
                return false;
            }
            return engine.isEnabledFor(entity) && getStepGenerator(engine).isPresent();
        }
    }
}
