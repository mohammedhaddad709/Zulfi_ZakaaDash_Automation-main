package com.taf.core;

import org.openqa.selenium.interactions.Actions;
import com.taf.core.drivers.GUIDriver;
import com.taf.core.media.BrowserVideoRecorder;
import com.taf.core.media.EvidenceRegistry;
import com.taf.core.media.ScreenshotsManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

/** Real-browser checks for evidence capture (local page, no network). Run with -DexecutionType=LocalHeadless. */
public class BrowserVideoRecorderTest {

    private static final String ANIMATED_PAGE = "<html><body style='font:40px sans-serif'><div id='t'>0</div><script>"
            + "let n=0;setInterval(()=>{document.getElementById('t').textContent=++n;"
            + "document.body.style.background=n%2?'#dfe8ff':'#fff3d6';},150);</script></body></html>";

    private GUIDriver session;

    @BeforeMethod
    public void openBrowser() {
        EvidenceRegistry.reset();
        session = new GUIDriver();
        session.get().get("data:text/html;base64,"
                + Base64.getEncoder().encodeToString(ANIMATED_PAGE.getBytes(StandardCharsets.UTF_8)));
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        BrowserVideoRecorder.stopAndDiscard();
        if (session != null) session.quitDriver();
    }

    @Test(description = "CORE-MEDIA-001 — Browser video is recorded in headless mode and registered as evidence")
    public void recordsPlayableMp4AndRegistersEvidence() throws Exception {
        WebDriver driver = session.get();
        Assert.assertTrue(BrowserVideoRecorder.start(driver, "CORE-MEDIA-001"), "recording must start on Chromium");
        new Actions(driver).pause(Duration.ofMillis(2500)).perform();
        Path video = BrowserVideoRecorder.stopAndSave("CORE-MEDIA-001_video");

        Assert.assertNotNull(video, "an MP4 must be produced");
        Assert.assertTrue(Files.size(video) > 10_000, "MP4 should contain real frames, size=" + Files.size(video));
        byte[] header = Files.readAllBytes(video);
        Assert.assertEquals(new String(header, 4, 4, StandardCharsets.US_ASCII), "ftyp", "file must be an MP4 container");
        Assert.assertEquals(EvidenceRegistry.current(EvidenceRegistry.Kind.VIDEO).size(), 1);
        Assert.assertEquals(EvidenceRegistry.current(EvidenceRegistry.Kind.VIDEO).getFirst().path(), video.toAbsolutePath().normalize());
        Assert.assertFalse(Files.exists(video.getParent().resolve("frames-CORE-MEDIA-001")), "temporary frames removed");
    }

    @Test(description = "CORE-MEDIA-002 — Discarded recordings leave no video; screenshots register their real path")
    public void discardLeavesNoVideoAndScreenshotsAreRegistered() {
        WebDriver driver = session.get();
        Assert.assertTrue(BrowserVideoRecorder.start(driver, "CORE-MEDIA-002"));
        BrowserVideoRecorder.stopAndDiscard();
        Assert.assertFalse(BrowserVideoRecorder.isRecording());
        Assert.assertTrue(EvidenceRegistry.current(EvidenceRegistry.Kind.VIDEO).isEmpty());

        Path shot = ScreenshotsManager.takeFullPageScreenshot(driver, "CORE-MEDIA-002_evidence");
        Assert.assertNotNull(shot);
        Assert.assertTrue(Files.isRegularFile(shot));
        Assert.assertEquals(EvidenceRegistry.current(EvidenceRegistry.Kind.SCREENSHOT).getFirst().path(), shot.toAbsolutePath().normalize());
    }
}
