package eu.ha3.presencefootsteps;

import java.util.Optional;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.Tooltip;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.common.client.gui.element.AbstractSlider;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.EnumSlider;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.client.gui.element.Slider;

import eu.ha3.mc.quick.update.Versions;
import eu.ha3.presencefootsteps.util.BlockReport;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

class PFOptionsScreen extends GameGui {
    public static final Text TITLE = Text.translatable("menu.pf.title");
    public static final Text UP_TO_DATE = Text.translatable("pf.update.up_to_date");
    public static final Text VOLUME_MIN = Text.translatable("menu.pf.volume.min");

    public PFOptionsScreen(@Nullable Screen parent) {
        super(TITLE, parent);
    }

    @Override
    public void init() {
        int left = width / 2 - 100;

        int wideLeft = width / 2 - 155;
        int wideRight = wideLeft + 160;

        int row = height / 4;

        PFConfig config = PresenceFootsteps.getInstance().getConfig();

        addButton(new Label(width / 2, 30)).setCentered().getStyle()
                .setText(getTitle());

        redrawUpdateButton(addButton(new Button(width - 45, 20, 25, 20)).onClick(sender -> {
            sender.setEnabled(false);
            sender.getStyle().setTooltip("pf.update.checking");
            PresenceFootsteps.getInstance().getUpdateChecker().checkNow().thenAccept(newVersions -> {
                redrawUpdateButton(sender);
            });
        }));

        var slider = addButton(new Slider(wideLeft, row, 0, 100, config.getGlobalVolume()))
            .onChange(config::setGlobalVolume)
            .setTextFormat(this::formatVolume);
        slider.setBounds(new Bounds(row, wideLeft, 310, 20));
        slider.getStyle().setTooltip(Tooltip.of("menu.pf.volume.tooltip", 210)).setTooltipOffset(0, 25);

        slider = addButton(new Slider(wideLeft, row += 24, 0, 100, config.getClientPlayerVolume()))
            .onChange(config::setClientPlayerVolume)
            .setTextFormat(formatVolume("menu.pf.volume.player"));
        slider.setBounds(new Bounds(row, wideLeft, 150, 20));
        slider.getStyle().setTooltip(Tooltip.of("menu.pf.volume.player.tooltip", 210)).setTooltipOffset(0, 25);

        slider = addButton(new Slider(wideRight, row, 0, 100, config.getOtherPlayerVolume()))
            .onChange(config::setOtherPlayerVolume)
            .setTextFormat(formatVolume("menu.pf.volume.other_players"));
        slider.setBounds(new Bounds(row, wideRight, 150, 20));
        slider.getStyle().setTooltip(Tooltip.of("menu.pf.volume.other_players.tooltip", 210)).setTooltipOffset(0, 25);

        slider = addButton(new Slider(wideLeft, row += 24, -100, 100, config.getRunningVolumeIncrease()))
            .onChange(config::setRunningVolumeIncrease)
            .setTextFormat(formatVolume("menu.pf.volume.running"));
        slider.setBounds(new Bounds(row, wideLeft, 310, 20));
        slider.getStyle().setTooltip(Tooltip.of("menu.pf.volume.running.tooltip", 210)).setTooltipOffset(0, 25);

        slider = addButton(new Slider(wideLeft, row += 24, 0, 100, config.getWetSoundsVolume()))
                .onChange(config::setWetSoundsVolume)
                .setTextFormat(formatVolume("menu.pf.volume.wet"));
            slider.setBounds(new Bounds(row, wideLeft, 310, 20));
            slider.getStyle().setTooltip(Tooltip.of("menu.pf.volume.wet.tooltip", 210)).setTooltipOffset(0, 25);

        addButton(new EnumSlider<>(left, row += 24, config.getLocomotion())
                .onChange(config::setLocomotion)
                .setTextFormat(v -> v.getValue().getOptionName()))
                .setTooltipFormat(v -> Tooltip.of(v.getValue().getOptionTooltip(), 250))
                .setBounds(new Bounds(row, wideLeft, 310, 20));

        addButton(new Button(wideLeft, row += 24, 150, 20).onClick(sender -> {
            sender.getStyle().setText("menu.pf.global." + config.cycleTargetSelector().name().toLowerCase());
        })).getStyle()
            .setText("menu.pf.global." + config.getEntitySelector().name().toLowerCase());

        addButton(new Button(wideRight, row, 150, 20).onClick(sender -> {
            sender.getStyle().setText("menu.pf.multiplayer." + config.toggleMultiplayer());
        })).getStyle()
            .setText("menu.pf.multiplayer." + config.getEnabledMP());

        addButton(new Button(wideLeft, row += 24, 150, 20).onClick(sender -> {
            sender.setEnabled(false);
            new BlockReport("report_concise")
                .execute(state -> !PresenceFootsteps.getInstance().getEngine().getIsolator().getBlockMap().contains(state))
                .thenRun(() -> sender.setEnabled(true));
        })).setEnabled(client.world != null)
            .getStyle()
            .setText("menu.pf.report.concise");

        addButton(new Button(wideRight, row, 150, 20)
            .onClick(sender -> {
                sender.setEnabled(false);
                new BlockReport("report_full")
                    .execute(null)
                    .thenRun(() -> sender.setEnabled(true));
            }))
            .setEnabled(client.world != null)
            .getStyle()
                .setText("menu.pf.report.full");

        addButton(new Button(left, row += 34)
            .onClick(sender -> finish())).getStyle()
            .setText("gui.done");
    }

    private void redrawUpdateButton(Button button) {
        Optional<Versions> versions = PresenceFootsteps.getInstance().getUpdateChecker().getNewer();
        boolean hasUpdate = versions.isPresent();
        button.setEnabled(true);
        button.getStyle()
           .setText(hasUpdate ? "!" : ":)")
           .setColor(hasUpdate ? 0xAAFF00 : 0xFFFFFF)
           .setTooltip(versions
                   .map(Versions::latest)
                   .map(latest -> (Text)Text.translatable("pf.update.updates_available",
                           latest.version().getFriendlyString(),
                           latest.minecraft().getFriendlyString()))
                   .orElse(UP_TO_DATE));
    }

    private Text formatVolume(AbstractSlider<Float> slider) {
        if (slider.getValue() <= 0) {
            return VOLUME_MIN;
        }

        return Text.translatable("menu.pf.volume", (int)Math.floor(slider.getValue()));
    }

    static Function<AbstractSlider<Float>, Text> formatVolume(String key) {
        return slider -> Text.translatable(key, (int)Math.floor(slider.getValue()));
    }
}
