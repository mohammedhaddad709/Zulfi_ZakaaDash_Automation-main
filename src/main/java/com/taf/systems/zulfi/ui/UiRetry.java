package com.taf.systems.zulfi.ui;

import org.openqa.selenium.StaleElementReferenceException;

import java.util.function.Supplier;

/**
 * Re-runs a whole lookup-and-act block when the portal's Vue lists re-render under it. The block must re-locate its
 * elements on every attempt; retrying a stale handle cannot work. Not a substitute for explicit waits.
 */
public final class UiRetry {

    public static final int DEFAULT_ATTEMPTS = 3;

    private UiRetry() {}

    public static <T> T onStale(Supplier<T> block) {
        return onStale(DEFAULT_ATTEMPTS, block);
    }

    public static <T> T onStale(int attempts, Supplier<T> block) {
        StaleElementReferenceException last = null;
        for (int attempt = 1; attempt <= Math.max(1, attempts); attempt++) {
            try {
                return block.get();
            } catch (StaleElementReferenceException stale) {
                last = stale;
            }
        }
        throw last;
    }

    public static void onStale(Runnable block) {
        onStale(DEFAULT_ATTEMPTS, () -> {
            block.run();
            return null;
        });
    }
}
