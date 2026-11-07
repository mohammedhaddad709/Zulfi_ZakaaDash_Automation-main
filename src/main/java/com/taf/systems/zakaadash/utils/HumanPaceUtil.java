package com.taf.systems.zakaadash.utils;

import com.taf.systems.zakaadash.config.ConfigManager;
import com.taf.systems.zakaadash.driver.DriverManager;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/** Human-like pauses via Selenium {@link Actions#pause(Duration)} (never {@code Thread.sleep}). */
public final class HumanPaceUtil {

    public static final Duration NORMAL_ACTION = Duration.ofMillis(1500);
    public static final Duration WIZARD_TRANSITION = Duration.ofMillis(2000);
    public static final Duration STEP5_RESULTS_VIEW = Duration.ofMillis(2500);

    private HumanPaceUtil() {
    }

    public static void pauseBetweenActions() {
        if (!ConfigManager.config().humanDelayEnabled()) {
            return;
        }
        int min = ConfigManager.config().humanDelayMinMs();
        int max = ConfigManager.config().humanDelayMaxMs();
        if (max < min) {
            max = min;
        }
        int delayMs = ThreadLocalRandom.current().nextInt(min, max + 1);
        pause(Duration.ofMillis(delayMs), "random-action");
    }

    public static void pauseNormalAction() {
        pause(NORMAL_ACTION, "normal-action");
    }

    public static void pauseWizardTransition() {
        pause(WIZARD_TRANSITION, "wizard-transition");
    }

    public static void pausePreprocessingAction() {
        pause(Duration.ofMillis(ConfigManager.config().humanDelayPreprocessingMs()), "preprocessing-action");
    }

    public static void pauseStep4TabSwitch() {
        if (!ConfigManager.config().humanDelayEnabled()) {
            return;
        }
        int min = ConfigManager.config().humanDelayStep4MinMs();
        int max = ConfigManager.config().humanDelayStep4MaxMs();
        if (max < min) {
            max = min;
        }
        int delayMs = ThreadLocalRandom.current().nextInt(min, max + 1);
        pause(Duration.ofMillis(delayMs), "step4-tab-switch");
    }

    public static void pauseStep5ResultsView() {
        pause(STEP5_RESULTS_VIEW, "step5-results-view");
    }

    public static void pause(Duration duration, String reason) {
        if (!ConfigManager.config().humanDelayEnabled()) {
            return;
        }
        System.out.println("[HumanPace] " + reason + " — pause " + duration.toMillis() + "ms");
        new Actions(DriverManager.getDriver()).pause(duration).perform();
    }
}
