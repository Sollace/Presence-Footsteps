package eu.ha3.presencefootsteps.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import com.google.gson.stream.JsonWriter;
import com.minelittlepony.common.util.GamePaths;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public interface BlockReport {
    static CompletableFuture<?> execute(Reportable reportable, String baseName, boolean full) {
        return execute(loc -> {
            try (var writer = JsonObjectWriter.of(new JsonWriter(Files.newBufferedWriter(loc)))) {
                reportable.writeToReport(full, writer, new Object2ObjectOpenHashMap<>());
            }
        }, baseName, ".json");
    }

    static CompletableFuture<?> execute(UnsafeConsumer<Path> action, String baseName, String ext) {
        MinecraftClient client = MinecraftClient.getInstance();
        ChatHud hud = client.inGameHud.getChatHud();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path loc = getUniqueFileName(GamePaths.getGameDirectory().resolve("presencefootsteps"), baseName, ext);
                action.accept(loc);
                return loc;
            } catch (Exception e) {
                throw new RuntimeException("Could not generate report", e);
            }
        }, Util.getIoWorkerExecutor()).thenAcceptAsync(loc -> {
            hud.addMessage(Text.translatable("pf.report.save", Text.literal(loc.getFileName().toString()).styled(s -> s
                    .withClickEvent(new ClickEvent.OpenFile(loc.toString()))
                    .withFormatting(Formatting.UNDERLINE)))
                .styled(s -> s
                    .withColor(Formatting.GREEN)));
        }, client).exceptionallyAsync(e -> {
            hud.addMessage(Text.translatable("pf.report.error", e.getMessage()).styled(s -> s.withColor(Formatting.RED)));
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
        void writeToReport(boolean full, JsonObjectWriter writer, Map<String, BlockSoundGroup> groups) throws IOException;
    }

    interface UnsafeConsumer<T> {
        void accept(T t) throws IOException;
    }
}
