package eu.ha3.presencefootsteps.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import com.google.gson.stream.JsonWriter;
import com.minelittlepony.common.util.GamePaths;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.SoundType;

public interface BlockReport {
    static CompletableFuture<?> execute(Reportable reportable, String baseName, boolean full) {
        return execute(loc -> {
            try (var writer = JsonObjectWriter.of(new JsonWriter(Files.newBufferedWriter(loc)))) {
                reportable.writeToReport(full, writer, new Object2ObjectOpenHashMap<>());
            }
        }, baseName, ".json");
    }

    static CompletableFuture<?> execute(UnsafeConsumer<Path> action, String baseName, String ext) {
        Minecraft client = Minecraft.getInstance();
        ChatComponent hud = client.gui.getChat();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path loc = getUniqueFileName(GamePaths.getGameDirectory().resolve("presencefootsteps"), baseName, ext);
                action.accept(loc);
                return loc;
            } catch (Exception e) {
                throw new RuntimeException("Could not generate report", e);
            }
        }, Util.ioPool()).thenAcceptAsync(loc -> {
            hud.addMessage(Component.translatable("pf.report.save", Component.literal(loc.getFileName().toString()).withStyle(s -> s
                    .withClickEvent(new ClickEvent.OpenFile(loc.toString()))
                    .withUnderlined(true)))
                .withStyle(s -> s
                    .withColor(ChatFormatting.GREEN)));
        }, client).exceptionallyAsync(e -> {
            hud.addMessage(Component.translatable("pf.report.error", e.getMessage()).withStyle(s -> s.withColor(ChatFormatting.RED)));
            return null;
        }, client);
    }

    private static Path getUniqueFileName(Path directory, String baseName, String ext) throws IOException {
        Path loc = null;

        int counter = 0;
        while (loc == null || Files.exists(loc)) {
            loc = directory.resolve(baseName + (counter == 0 ? "" : "_" + counter) + ext);
            counter++;
        }

        Files.createDirectories(ext.isEmpty() ? loc : loc.getParent());
        return loc;
    }

    interface Reportable {
        void writeToReport(boolean full, JsonObjectWriter writer, Map<String, SoundType> groups) throws IOException;
    }

    interface UnsafeConsumer<T> {
        void accept(T t) throws IOException;
    }
}
