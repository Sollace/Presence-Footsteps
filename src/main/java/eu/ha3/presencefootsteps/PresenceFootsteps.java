package eu.ha3.presencefootsteps;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class PresenceFootsteps implements ClientModInitializer {
    public static final Logger logger = LogManager.getLogger("PFSolver");

    private static final String MODID = "presencefootsteps";
    private static final String KEY_BINDING_CATEGORY = "key.category." + MODID;
    private static final String UPDATER_ENDPOINT = "https://raw.githubusercontent.com/Sollace/Presence-Footsteps/master/version/latest.json";

    public static final Text MOD_NAME = Text.translatable("mod.presencefootsteps.name");

    public static Identifier id(String name) {
        return Identifier.of(MODID, name);
    }

    private static PresenceFootsteps instance;

    public static PresenceFootsteps getInstance() {
        return instance;
    }

    private SoundEngine engine;

    private PFConfig config;

    private PFDebugHud debugHud;

    private UpdateChecker updater;

    private KeyBinding optionsKeyBinding;
    private KeyBinding toggleKeyBinding;
    private boolean toggleTriggered;

    private final AtomicBoolean configChanged = new AtomicBoolean();

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

    public KeyBinding getOptionsKeyBinding() {
        return optionsKeyBinding;
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
        config.onChangedExternally(c -> configChanged.set(true));

        optionsKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.presencefootsteps.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F10, KEY_BINDING_CATEGORY));
        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.presencefootsteps.toggle", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_BINDING_CATEGORY));

        engine = new SoundEngine(config);
        debugHud = new PFDebugHud(engine);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(engine);
    }

    private void onTick(MinecraftClient client) {
        if (client.currentScreen instanceof PFOptionsScreen screen && configChanged.getAndSet(false)) {
            screen.init(client, screen.width, screen.height);
        }

        Optional.ofNullable(client.player).filter(e -> !e.isRemoved()).ifPresent(cameraEntity -> {

            if (client.currentScreen == null) {
                if (optionsKeyBinding.isPressed()) {
                    client.setScreen(new PFOptionsScreen(client.currentScreen));
                }
                if (toggleKeyBinding.isPressed()) {
                    if (!toggleTriggered) {
                        toggleTriggered = true;
                        config.toggleDisabled();
                    }
                } else {
                    toggleTriggered = false;
                }
            }

            engine.onFrame(client, cameraEntity);

            if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
                updater.attempt();
            }
        });
    }

    private void onUpdate(TargettedVersion newVersion, TargettedVersion currentVersion) {
        showSystemToast(
                Text.translatable("pf.update.title"),
                Text.translatable("pf.update.text", newVersion.version().getFriendlyString(), newVersion.minecraft().getFriendlyString())
        );
    }

    void onEnabledStateChange(boolean enabled) {
        engine.reload();
        showSystemToast(
                MOD_NAME,
                Text.translatable("key.presencefootsteps.toggle." + (enabled ? "enabled" : "disabled")).formatted(enabled ? Formatting.GREEN : Formatting.GRAY)
        );
    }

    public void showSystemToast(Text title, Text body) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.getToastManager().add(SystemToast.create(client, SystemToast.Type.PACK_LOAD_FAILURE, title, body));
    }
}
