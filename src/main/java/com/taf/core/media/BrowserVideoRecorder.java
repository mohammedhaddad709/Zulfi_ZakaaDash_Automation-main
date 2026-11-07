package com.taf.core.media;

import com.taf.core.utils.OutputPaths;
import com.taf.core.utils.logs.LogsManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.Event;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonInput;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Records the browser page itself (not the desktop) through the Chrome DevTools screencast, so video evidence
 * also works in headless runs. Frames are encoded to MP4 with the ffmpeg binary bundled by JAVE.
 *
 * <p>Chromium only (Chrome/Edge, local or headless). On Firefox/Safari/Grid it logs a warning and records nothing;
 * recording never fails a test. Only the tab that was active when recording started is captured.
 */
public final class BrowserVideoRecorder {

    private static final ThreadLocal<BrowserVideoRecorder> CURRENT = new ThreadLocal<>();
    private static final double LAST_FRAME_SECONDS = 1.5;

    private final DevTools devTools;
    private final Path framesDir;
    private final List<Frame> frames = new ArrayList<>();
    private final ExecutorService acker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "screencast-ack");
        t.setDaemon(true);
        return t;
    });

    private record Frame(Path file, double timestamp) { }

    private BrowserVideoRecorder(DevTools devTools, Path framesDir) {
        this.devTools = devTools;
        this.framesDir = framesDir;
    }

    /** Starts recording the driver's current tab; returns false (with a warning) when unsupported. */
    public static boolean start(WebDriver driver, String testName) {
        stopAndDiscard();
        HasDevTools devToolsDriver = asDevTools(driver);
        if (devToolsDriver == null) {
            LogsManager.warn("Browser video unavailable: driver has no DevTools (Chromium local/headless only)");
            return false;
        }
        try {
            DevTools devTools = devToolsDriver.getDevTools();
            devTools.createSessionIfThereIsNotOne();
            Path dir = OutputPaths.recordings().resolve("frames-" + safe(testName) + "-" + System.nanoTime());
            Files.createDirectories(dir);
            BrowserVideoRecorder recorder = new BrowserVideoRecorder(devTools, dir);
            devTools.addListener(new Event<>("Page.screencastFrame", BrowserVideoRecorder::readMap), recorder::onFrame);
            devTools.send(new Command<>("Page.startScreencast", Map.of(
                    "format", "jpeg", "quality", 70, "maxWidth", 1366, "maxHeight", 900, "everyNthFrame", 1)));
            CURRENT.set(recorder);
            return true;
        } catch (RuntimeException | IOException ex) {
            LogsManager.warn("Browser video could not start: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return false;
        }
    }

    public static boolean isRecording() {
        return CURRENT.get() != null;
    }

    /** Stops recording and encodes an MP4 named after the test; returns null when nothing was captured. */
    public static Path stopAndSave(String videoName) {
        BrowserVideoRecorder recorder = CURRENT.get();
        CURRENT.remove();
        if (recorder == null) return null;
        try {
            recorder.stopScreencast();
            Path video = recorder.encode(OutputPaths.recordings().resolve(safe(videoName) + ".mp4"));
            if (video != null) EvidenceRegistry.record(EvidenceRegistry.Kind.VIDEO, videoName, video);
            return video;
        } finally {
            recorder.deleteFrames();
        }
    }

    /** Stops recording and deletes the frames (passing test with ON_FAILURE). */
    public static void stopAndDiscard() {
        BrowserVideoRecorder recorder = CURRENT.get();
        CURRENT.remove();
        if (recorder != null) {
            recorder.stopScreencast();
            recorder.deleteFrames();
        }
    }

    private void onFrame(Map<String, Object> params) {
        try {
            Object sessionId = params.get("sessionId");
            acker.submit(() -> {
                try {
                    devTools.send(new Command<>("Page.screencastFrameAck", Map.of("sessionId", sessionId)));
                } catch (RuntimeException ignored) {
                    // the page may already be closing
                }
            });
            byte[] jpeg = Base64.getDecoder().decode(String.valueOf(params.get("data")));
            double timestamp = timestampOf(params);
            synchronized (frames) {
                Path file = framesDir.resolve(String.format("frame_%06d.jpg", frames.size()));
                Files.write(file, jpeg);
                frames.add(new Frame(file, timestamp));
            }
        } catch (IOException | RuntimeException ex) {
            LogsManager.warn("Screencast frame dropped: " + ex.getClass().getSimpleName());
        }
    }

    private void stopScreencast() {
        try {
            devTools.send(new Command<>("Page.stopScreencast", Map.of()));
        } catch (RuntimeException ignored) {
            // browser may already be gone after a crash
        }
        try {
            devTools.clearListeners();
        } catch (RuntimeException ignored) {
            // best effort
        }
        acker.shutdown();
        try {
            acker.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private Path encode(Path output) {
        List<Frame> snapshot;
        synchronized (frames) {
            snapshot = new ArrayList<>(frames);
        }
        if (snapshot.isEmpty()) {
            LogsManager.warn("Browser video: no frames captured");
            return null;
        }
        try {
            Files.createDirectories(output.getParent());
            StringBuilder list = new StringBuilder();
            for (int i = 0; i < snapshot.size(); i++) {
                double duration = i + 1 < snapshot.size()
                        ? Math.max(0.04, snapshot.get(i + 1).timestamp() - snapshot.get(i).timestamp())
                        : LAST_FRAME_SECONDS;
                list.append("file '").append(snapshot.get(i).file().getFileName()).append("'\n")
                        .append("duration ").append(String.format(Locale.ROOT, "%.3f", duration)).append('\n');
            }
            // concat demuxer needs the last file repeated for its duration to apply
            list.append("file '").append(snapshot.getLast().file().getFileName()).append("'\n");
            Path listFile = framesDir.resolve("frames.txt");
            Files.writeString(listFile, list, StandardCharsets.UTF_8);
            Process process = new ProcessBuilder(
                    new DefaultFFMPEGLocator().getExecutablePath(), "-y", "-loglevel", "error",
                    "-f", "concat", "-safe", "0", "-i", listFile.toString(),
                    "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2", "-vsync", "vfr", "-pix_fmt", "yuv420p",
                    "-c:v", "libx264", "-preset", "veryfast", "-movflags", "+faststart", output.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(framesDir.resolve("ffmpeg.log").toFile())
                    .start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                LogsManager.warn("Browser video encoding timed out");
                return null;
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(output) || Files.size(output) == 0) {
                LogsManager.warn("Browser video encoding failed: " + Files.readString(framesDir.resolve("ffmpeg.log")).trim());
                return null;
            }
            return output;
        } catch (IOException | RuntimeException ex) {
            LogsManager.warn("Browser video encoding failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return null;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void deleteFrames() {
        try (var paths = Files.walk(framesDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // temporary frames only
        }
    }

    private static HasDevTools asDevTools(WebDriver driver) {
        WebDriver current = driver;
        for (int depth = 0; depth < 3 && current != null; depth++) {
            if (current instanceof HasDevTools devTools) return devTools;
            current = current instanceof WrapsDriver wraps ? wraps.getWrappedDriver() : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readMap(JsonInput input) {
        return (Map<String, Object>) input.read(Json.MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    private static double timestampOf(Map<String, Object> params) {
        Object metadata = params.get("metadata");
        if (metadata instanceof Map<?, ?> map && map.get("timestamp") instanceof Number number) {
            return number.doubleValue();
        }
        return System.nanoTime() / 1_000_000_000.0;
    }

    private static String safe(String value) {
        return value == null ? "test" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
