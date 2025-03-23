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
        MinecraftClient client = MinecraftClient.getInstance();
        ChatHud hud = client.inGameHud.getChatHud();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path loc = getUniqueFileName(GamePaths.getGameDirectory().resolve("presencefootsteps"), baseName, ".json");
                Files.createDirectories(loc.getParent());
                try (var writer = JsonObjectWriter.of(new JsonWriter(Files.newBufferedWriter(loc)))) {
                    reportable.writeToReport(full, writer, new Object2ObjectOpenHashMap<>());
                }
                return loc;
            } catch (IOException e) {
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

    private static Path getUniqueFileName(Path directory, String baseName, String ext) {
        Path loc = null;

        int counter = 0;
        while (loc == null || Files.exists(loc)) {
            loc = directory.resolve(baseName + (counter == 0 ? "" : "_" + counter) + ext);
            counter++;
        }

        return loc;
    }

    interface Reportable {
        void writeToReport(boolean full, JsonObjectWriter writer, Map<String, BlockSoundGroup> groups) throws IOException;
    }
}
