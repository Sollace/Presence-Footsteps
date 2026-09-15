package eu.ha3.presencefootsteps;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.minelittlepony.common.util.GamePaths;
import com.mojang.blaze3d.platform.InputConstants;

import eu.ha3.mc.quick.update.UpdateChecker;
import eu.ha3.mc.quick.update.UpdaterConfig;
import eu.ha3.presencefootsteps.sound.SoundEngine;
import eu.ha3.presencefootsteps.util.Edge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;

public class PresenceFootsteps implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("PFSolver");

    private static final String MODID = "presencefootsteps";
    private static final KeyMapping.Category KEY_BINDING_CATEGORY = KeyMapping.Category.register(id("category"));
    private static final String UPDATER_ENDPOINT = "https://raw.githubusercontent.com/Sollace/Presence-Footsteps/master/version/latest.json";

    public static final Component MOD_NAME = Component.translatable("mod.presencefootsteps.name");

    public static final SystemToast.SystemToastId PF_TOGGLED_TOAST_ID = new SystemToast.SystemToastId();

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MODID, name);
    }

    private static PresenceFootsteps instance;

    public static PresenceFootsteps getInstance() {
        return instance;
    }

    private final Path pfFolder = GamePaths.getConfigDirectory().resolve("presencefootsteps");
    private final PFConfig config = new PFConfig(pfFolder.resolve("userconfig.json"), this);
    private final SoundEngine engine = new SoundEngine(config);
    private final PFDebugHud debugHud = new PFDebugHud(engine);

    private final UpdaterConfig updaterConfig = new UpdaterConfig(pfFolder.resolve("updater.json"));
    private final UpdateChecker updater = new UpdateChecker(updaterConfig, MODID, UPDATER_ENDPOINT, (newVersion, _) -> {
        showSystemToast(
                Component.translatable("pf.update.title"),
                Component.translatable("pf.update.text", newVersion.version().getFriendlyString(), newVersion.minecraft().getFriendlyString())
        );
    });

    private final KeyMapping optionsKeyBinding = new KeyMapping("key.presencefootsteps.settings", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), KEY_BINDING_CATEGORY);
    private final KeyMapping toggleKeyBinding = new KeyMapping("key.presencefootsteps.toggle", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), KEY_BINDING_CATEGORY);
    private final KeyMapping debugToggleKeyBinding = new KeyMapping("key.presencefootsteps.debug_toggle", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), KEY_BINDING_CATEGORY, -1);
    private final Edge toggler = new Edge(z -> {
        if (z) {
            config.toggleDisabled();
        }
    });
    private final Edge debugToggle = new Edge(z -> {
        if (z) {
            boolean status = Minecraft.getInstance().debugEntries.toggleStatus(PFDebugHud.ID);
            if (status) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.translatable("pf.debug.toggled", Minecraft.getInstance().options.keyDebugModifier.getTranslatedKeyMessage(), debugToggleKeyBinding.getTranslatedKeyMessage()).withColor(TextColor.YELLOW)
                );
            }
        }
    });

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

    public KeyMapping getOptionsKeyBinding() {
        return optionsKeyBinding;
    }

    public UpdateChecker getUpdateChecker() {
        return updater;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onInitializeClient() {
        updaterConfig.load();
        config.load();
        config.onChangedExternally(_ -> configChanged.set(true));

        KeyMappingHelper.registerKeyMapping(optionsKeyBinding);
        KeyMappingHelper.registerKeyMapping(toggleKeyBinding);
        KeyMappingHelper.registerKeyMapping(debugToggleKeyBinding);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(SoundEngine.ID, engine);
        DebugScreenEntries.register(PFDebugHud.ID, debugHud);
        DebugScreenEntries.register(SoundEngine.DEBUG_VISUALISER_ID, new DebugEntryNoop());
    }

    private void onTick(Minecraft client) {
        if (client.gui.screen() instanceof PFOptionsScreen screen && configChanged.getAndSet(false)) {
            screen.init(screen.width, screen.height);
        }

        debugToggle.accept(client.options.keyDebugModifier.isDown() && debugToggleKeyBinding.isDown());
        config.setVisualiserRunning(client.debugEntries.isCurrentlyEnabled(SoundEngine.DEBUG_VISUALISER_ID));

        Optional.ofNullable(client.player).filter(e -> !e.isRemoved()).ifPresent(cameraEntity -> {
            if (client.gui.screen() == null) {
                if (optionsKeyBinding.isDown()) {
                    client.gui.setScreen(new PFOptionsScreen(client.gui.screen()));
                }
                toggler.accept(toggleKeyBinding.isDown());
            }

            engine.onFrame(client, cameraEntity);

            if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
                updater.attempt();
            }
        });
    }

    void onEnabledStateChange(boolean enabled) {
        engine.reload();
        showSystemToast(
                MOD_NAME,
                Component.translatable("key.presencefootsteps.toggle." + (enabled ? "enabled" : "disabled")).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY)
        );
    }

    public void showSystemToast(Component title, Component body) {
        Minecraft client = Minecraft.getInstance();
        SystemToast.addOrUpdate(client.gui.toastManager(), PF_TOGGLED_TOAST_ID, title, body);
    }
}
