package eu.ha3.presencefootsteps;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.google.gson.FormattingStyle;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.ScrollContainer;
import com.minelittlepony.common.client.gui.Tooltip;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.common.client.gui.element.AbstractSlider;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.EnumSlider;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.client.gui.element.Slider;
import com.minelittlepony.common.client.gui.element.Toggle;
import com.mojang.serialization.JsonOps;

import eu.ha3.mc.quick.update.Versions;
import eu.ha3.presencefootsteps.config.VolumeOption;
import eu.ha3.presencefootsteps.sound.acoustics.Acoustic;
import eu.ha3.presencefootsteps.sound.acoustics.AcousticsFile;
import eu.ha3.presencefootsteps.util.BlockReport;
import eu.ha3.presencefootsteps.util.ResourceUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;

class PFOptionsScreen extends GameGui {
    public static final Component TITLE = Component.translatable("menu.pf.title");
    public static final Component UP_TO_DATE = Component.translatable("pf.update.up_to_date");
    public static final Component VOLUME_MIN = Component.translatable("menu.pf.volume.min");

    private final ScrollContainer content = new ScrollContainer();

    public PFOptionsScreen(@Nullable Screen parent) {
        super(Component.translatable("%s (%s)", TITLE, PresenceFootsteps.getInstance().getOptionsKeyBinding().getTranslatedKeyMessage()), parent);
        content.margin.top = 30;
        content.margin.bottom = 30;
        content.getContentPadding().top = 10;
        content.getContentPadding().right = 10;
        content.getContentPadding().bottom = 20;
        content.getContentPadding().left = 10;
    }

    @Override
    protected void init() {
        content.init(this::rebuildContent);
    }

    private void rebuildContent() {
        int left = content.width / 2 - 100;

        int wideLeft = content.width / 2 - 165;
        int wideRight = wideLeft + 160;

        int row = 0;

        PFConfig config = PresenceFootsteps.getInstance().getConfig();

        getChildElements().add(content);

        addButton(new Label(width / 2, 10)).setCentered().getStyle().setText(getTitle());

        redrawUpdateButton(addButton(new Button(width - 30, height - 25, 25, 20)).onClick(sender -> {
            sender.setEnabled(false);
            sender.getStyle().setTooltip("pf.update.checking");
            PresenceFootsteps.getInstance().getUpdateChecker().checkNow().thenAccept(newVersions -> {
                redrawUpdateButton(sender);
            });
        }));

        Toggle disabledToggle = new Toggle(wideLeft, row, config.getDisabled());
        content.addButton(disabledToggle.onChange(disabled -> {
            updateDisableState(disabledToggle, config.setDisabled(disabled));
            return disabled;
        })).getStyle().setText("menu.pf.disable_mod");

        content.addButton(new Label(wideLeft, row += 24)).getStyle().setText("menu.pf.group.volume");

        var slider = content.addButton(new Slider(wideLeft, row += 24, 0, 100, config.getGlobalVolume()))
            .onChange(config::setGlobalVolume)
            .setTextFormat(this::formatVolume);
        slider.setBounds(new Bounds(row, wideLeft, 310, 20));
        slider.getStyle().setTooltip(Tooltip.of("menu.pf.volume.tooltip", 210)).setTooltipOffset(0, 25);

        row += 10;

        addVolumeSlider(wideLeft, row += 24, config.clientPlayerVolume, "player");
        addVolumeSlider(wideRight, row, config.otherPlayerVolume, "other_players");

        addVolumeSlider(wideLeft, row += 24, config.hostileEntitiesVolume, "hostile_entities");
        addVolumeSlider(wideRight, row, config.passiveEntitiesVolume, "passive_entities");

        addVolumeSlider(wideLeft, row += 24, config.wetSoundsVolume, "wet");
        addVolumeSlider(wideRight, row, config.foliageSoundsVolume, "foliage");
        row += 10;

        slider = content.addButton(new Slider(wideLeft, row += 24, -100, 100, config.getRunningVolumeIncrease()))
            .onChange(config::setRunningVolumeIncrease)
            .setTextFormat(formatVolume("menu.pf.volume.running"));
        slider.setBounds(new Bounds(row, wideLeft, 310, 20));
        slider.getStyle().setTooltip(Tooltip.of("menu.pf.volume.running.tooltip", 210)).setTooltipOffset(0, 25);

        content.addButton(new Label(wideLeft, row += 25)).getStyle().setText("menu.pf.group.footsteps");

        content.addButton(new EnumSlider<>(left, row += 24, config.getLocomotion())
                .onChange(config::setLocomotion)
                .setTextFormat(v -> v.getValue().getOptionName()))
                .setTooltipFormat(v -> Tooltip.of(v.getValue().getOptionTooltip(), 250))
                .setBounds(new Bounds(row, wideLeft, 310, 20));

        content.addButton(new Button(wideLeft, row += 24, 150, 20).onClick(sender -> {
            sender.getStyle().setText("menu.pf.global." + config.cycleTargetSelector().name().toLowerCase());
        })).getStyle()
            .setText("menu.pf.global." + config.getEntitySelector().name().toLowerCase());

        content.addButton(new Button(wideRight, row, 150, 20).onClick(sender -> {
            sender.getStyle().setText("menu.pf.multiplayer." + config.toggleMultiplayer());
        })).getStyle()
            .setText("menu.pf.multiplayer." + config.getEnabledMP());

        content.addButton(new Button(wideLeft, row += 24, 150, 20).onClick(sender -> {
            sender.getStyle().setText("menu.pf.footwear." + (config.toggleFootwear() ? "on" : "off"));
        })).getStyle()
            .setText("menu.pf.footwear." + (config.getEnabledFootwear() ? "on" : "off"));

        content.addButton(new Button(wideRight, row, 150, 20).onClick(sender -> {
            sender.getStyle().setText("menu.pf.exclusive_mode." + (config.toggleExclusiveMode() ? "on" : "off"));
        })).getStyle()
            .setText("menu.pf.exclusive_mode." + (config.isExclusiveMode() ? "on" : "off"));

        content.addButton(new Label(wideLeft, row += 25)).getStyle().setText("menu.pf.group.sound_packs");

        content.addButton(new Button(wideLeft, row += 25, 150, 20).onClick(sender -> {
            minecraft.setScreen(new PackSelectionScreen(
                    minecraft.getResourcePackRepository(),
                    repo -> {
                        minecraft.options.loadSelectedResourcePacks(repo);
                        minecraft.setScreen(this);
                    },
                    minecraft.getResourcePackDirectory(),
                    Component.translatable("resourcePack.title")
            ));
        })).getStyle().setText("options.resourcepack");

        content.addButton(new Label(wideLeft, row += 25)).getStyle().setText("menu.pf.group.debugging");

        content.addButton(new Button(wideLeft, row += 25, 150, 20).onClick(sender -> {
            sender.setEnabled(false);
            BlockReport.execute(PresenceFootsteps.getInstance().getEngine().getIsolator(), "report_concise", false).thenRun(() -> sender.setEnabled(true));
        })).setEnabled(minecraft.level != null)
            .getStyle()
            .setText("menu.pf.report.concise")
            .setTooltip("menu.pf.report.concise.tooltip");

        content.addButton(new Button(wideRight, row, 150, 20)
            .onClick(sender -> {
                sender.setEnabled(false);
                BlockReport.execute(PresenceFootsteps.getInstance().getEngine().getIsolator(), "report_full", true).thenRun(() -> sender.setEnabled(true));
            }))
            .setEnabled(minecraft.level != null)
            .getStyle()
                .setText("menu.pf.report.full")
                .setTooltip("menu.pf.report.full.tooltip");

        content.addButton(new Button(wideLeft, row += 25, 150, 20)
                .onClick(sender -> {
                    sender.setEnabled(false);
                    BlockReport.execute(loc -> {
                        ResourceUtils.forEach(AcousticsFile.FILE_LOCATION, minecraft.getResourceManager(), reader -> {
                            Map<String, Acoustic> acoustics = new HashMap<>();
                            @SuppressWarnings("deprecation")
                            AcousticsFile file = AcousticsFile.read(reader, acoustics::put, true);
                            if (file != null) {
                                for (var acoustic : acoustics.entrySet()) {
                                    Acoustic.CODEC.encodeStart(JsonOps.INSTANCE, acoustic.getValue()).resultOrPartial(error -> {
                                        PresenceFootsteps.logger.error("Error whilst exporting acoustic: " + error);
                                    }).ifPresent(json -> {
                                        try (var writer = new JsonWriter(Files.newBufferedWriter(loc.resolve(acoustic.getKey().toLowerCase(Locale.ROOT) + ".json")))) {
                                            writer.setFormattingStyle(FormattingStyle.PRETTY);
                                            Streams.write(json, writer);
                                        } catch (IOException e) {
                                            PresenceFootsteps.logger.error("Error whilst exporting acoustics", e);
                                        }
                                    });
                                }
                            }
                        });
                    }, "acoustics", "").thenRun(() -> sender.setEnabled(true));
                }))
                .setEnabled(minecraft.level != null)
                .getStyle()
                    .setText("menu.pf.report.acoustics")
                    .setTooltip("menu.pf.report-acoustics.tooltip");

        addButton(new Button(left, height - 25)
            .onClick(sender -> finish())).getStyle()
            .setText("gui.done");

        updateDisableState(disabledToggle, disabledToggle.getValue());
    }

    private void addVolumeSlider(int x, int y, VolumeOption option, String name) {
        var slider = content.addButton(new Slider(x, y, 0, 100, option.get()))
                .onChange(option)
                .setTextFormat(formatVolume("menu.pf.volume." + name));
        slider.setBounds(new Bounds(y, x, 150, 20));
        slider.styled(s -> s.setTooltip(Tooltip.of("menu.pf.volume." + name + ".tooltip", 210)).setTooltipOffset(0, 25));
    }

    private void updateDisableState(Toggle disabledToggle, boolean disabled) {
        content.children().forEach(child -> {
            if (child != disabledToggle && child instanceof Button button) {
                button.setEnabled(!disabled);
            }
        });
    }

    private void redrawUpdateButton(Button button) {
        Optional<Versions> versions = PresenceFootsteps.getInstance().getUpdateChecker().getNewer();
        boolean hasUpdate = versions.isPresent();
        button.setEnabled(true);
        button.getStyle()
           .setText(hasUpdate ? "🙁" : "🙂")
           .setColor(hasUpdate ? 0xFF0000 : 0xFFFFFF)
           .setTooltip(versions
                   .map(Versions::latest)
                   .map(latest -> (Component)Component.translatable("pf.update.updates_available",
                           latest.version().getFriendlyString(),
                           latest.minecraft().getFriendlyString()))
                   .orElse(UP_TO_DATE));
    }

    private Component formatVolume(AbstractSlider<Float> slider) {
        if (slider.getValue() <= 0) {
            return VOLUME_MIN;
        }

        return Component.translatable("menu.pf.volume", (int)Math.floor(slider.getValue()));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float tickDelta) {
        super.render(context, mouseX, mouseY, tickDelta);
        content.render(context, mouseX, mouseY, tickDelta);
    }

    static Function<AbstractSlider<Float>, Component> formatVolume(String key) {
        return slider -> Component.translatable(key, (int)Math.floor(slider.getValue()));
    }
}
