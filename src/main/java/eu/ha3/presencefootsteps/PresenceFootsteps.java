package eu.ha3.presencefootsteps;

import java.nio.file.Path;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import com.minelittlepony.common.util.GamePaths;

import eu.ha3.mc.quick.update.TargettedVersion;
import eu.ha3.mc.quick.update.UpdateChecker;
import eu.ha3.mc.quick.update.UpdaterConfig;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PresenceFootsteps implements ClientModInitializer {
    public static final Logger logger = LogManager.getLogger("PFSolver");

    private static final String MODID = "presencefootsteps";
    private static final String UPDATER_ENDPOINT = "https://raw.githubusercontent.com/Sollace/Presence-Footsteps/master/version/latest.json";

    private static PresenceFootsteps instance;

    public static PresenceFootsteps getInstance() {
        return instance;
    }

    private SoundEngine engine;

    private PFConfig config;

    private PFDebugHud debugHud;

    private UpdateChecker updater;

    private KeyBinding keyBinding;

    public PresenceFootsteps() {
        instance = this;
    }

    public PFDebugHud getDebugHud() {
        return debugHud;
    }

    public SoundEngine getEngine() {
        return engine;
    }

    public PFConfig getConfig() {
        return config;
    }

    public UpdateChecker getUpdateChecker() {
        return updater;
    }

    @Override
    public void onInitializeClient() {
        Path pfFolder = GamePaths.getConfigDirectory().resolve("presencefootsteps");

        updater = new UpdateChecker(new UpdaterConfig(pfFolder.resolve("updater.json")), MODID, UPDATER_ENDPOINT, this::onUpdate);

        config = new PFConfig(pfFolder.resolve("userconfig.json"), this);
        config.load();

        keyBinding = new KeyBinding("key.presencefootsteps.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F10, "key.categories.misc");

        KeyBindingHelper.registerKeyBinding(keyBinding);

        engine = new SoundEngine(config);
        debugHud = new PFDebugHud(engine);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(engine);

        FabricLoader.getInstance().getModContainer("presencefootsteps").ifPresent(container -> {
            ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("presencefootsteps", "default_sound_pack"), container, Text.translatable("pf.default_sounds.name"), ResourcePackActivationType.DEFAULT_ENABLED);
        });
    }

    private void onTick(MinecraftClient client) {
        Optional.ofNullable(client.player).filter(e -> !e.isRemoved()).ifPresent(cameraEntity -> {
            if (keyBinding.isPressed() && client.currentScreen == null) {
                client.setScreen(new PFOptionsScreen(client.currentScreen));
            }

            engine.onFrame(client, cameraEntity);
            updater.attempt();
        });
    }

    private void onUpdate(TargettedVersion newVersion, TargettedVersion currentVersion) {
        ToastManager manager = MinecraftClient.getInstance().getToastManager();

        SystemToast.add(manager, SystemToast.Type.TUTORIAL_HINT,
                Text.translatable("pf.update.title"),
                Text.translatable("pf.update.text", newVersion.version().getFriendlyString(), newVersion.minecraft().getFriendlyString()));
    }
}
