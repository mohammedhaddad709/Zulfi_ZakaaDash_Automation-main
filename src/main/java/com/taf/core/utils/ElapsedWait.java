package com.taf.core.utils;

import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;

/** Fluent polling wait for a minimum elapsed duration (replaces fixed Thread.sleep). */
public final class ElapsedWait {

    private ElapsedWait() {}

    public static void atLeast(Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return;
        }
        long deadlineNanos = System.nanoTime() + duration.toNanos();
        new FluentWait<>(deadlineNanos)
                .withTimeout(duration.plusMillis(500))
                .pollingEvery(Duration.ofMillis(50))
                .until(deadline -> System.nanoTime() >= deadline);
    }
}
