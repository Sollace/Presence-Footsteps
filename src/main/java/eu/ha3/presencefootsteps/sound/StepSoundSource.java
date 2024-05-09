package eu.ha3.presencefootsteps.sound;

import java.util.Optional;

import eu.ha3.presencefootsteps.PresenceFootsteps;
import eu.ha3.presencefootsteps.sound.generator.Locomotion;
import eu.ha3.presencefootsteps.sound.generator.StepSoundGenerator;
import net.minecraft.entity.LivingEntity;

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
            return PresenceFootsteps.getInstance().getEngine().isEnabledFor(entity)
                    && getStepGenerator(PresenceFootsteps.getInstance().getEngine()).isPresent();
        }
    }
}
