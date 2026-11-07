package com.taf.core.media;

import com.taf.core.utils.OutputPaths;
import com.automation.remarks.video.RecorderFactory;
import com.automation.remarks.video.recorder.IVideoRecorder;
import com.automation.remarks.video.recorder.VideoRecorder;
import com.taf.core.utils.dataReader.PropertyReader;
import com.taf.core.utils.logs.LogsManager;
import ws.schild.jave.*;
import ws.schild.jave.encode.*;
import java.nio.file.*;
import java.io.File;

/** Optional recording based on the original TAF media capability. */
public final class ScreenRecordManager {
    private static final ThreadLocal<IVideoRecorder> RECORDER = new ThreadLocal<>();
    private ScreenRecordManager() { }
    public static void startRecording() {
        String mode = PropertyReader.getProperty("report.video");
        if (mode.isBlank() || "OFF".equalsIgnoreCase(mode)) return;
        if (!"Local".equalsIgnoreCase(PropertyReader.getProperty("executionType"))) return;
        try {
            Files.createDirectories(OutputPaths.recordings());
            System.setProperty("video.folder", OutputPaths.recordings().toString());
            RECORDER.set(RecorderFactory.getRecorder(VideoRecorder.conf().recorderType()));
            RECORDER.get().start();
        } catch (Throwable ex) {
            LogsManager.warn(
                    "Optional screen recording could not start: "
                            + ex.getClass().getSimpleName()
                            + ": "
                            + ex.getMessage());
            RECORDER.remove();
        }
    }
    /** Stop recording and return the ACTUAL mp4 path. Null if capture failed. */
    public static Path stopRecording(String testMethodName) {
        IVideoRecorder instance = RECORDER.get();
        if (instance == null) return null;
        try {
            String safe = testMethodName.replaceAll("[^a-zA-Z0-9._-]", "_");
            File recording = new File(String.valueOf(instance.stopAndSave(safe)));
            if (recording == null || !recording.isFile()) return null;
            File mp4 = new File(recording.getParentFile(), recording.getName().replaceFirst("\\.[^.]+$", ".mp4"));
            if (recording.getName().toLowerCase().endsWith(".mp4")) return recording.toPath();
            VideoAttributes video = new VideoAttributes();
            video.setCodec("libx264");
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp4"); attrs.setVideoAttributes(video);
            new Encoder().encode(new MultimediaObject(recording), mp4, attrs);
            if (mp4.isFile()) { Files.deleteIfExists(recording.toPath()); return mp4.toPath(); }
            return null;
        } catch (Exception ex) {
            LogsManager.warn("Optional screen recording failed: " + ex.getClass().getSimpleName());
            return null;
        } finally { RECORDER.remove(); }
    }
}
