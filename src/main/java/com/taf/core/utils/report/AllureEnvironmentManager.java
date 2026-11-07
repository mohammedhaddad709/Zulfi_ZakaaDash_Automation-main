package com.taf.core.utils.report;

import com.taf.core.utils.OutputPaths;
import com.taf.core.pacing.ExecutionPace;
import com.taf.core.utils.dataReader.PropertyReader;
import java.nio.file.*;
import java.io.IOException;

public final class AllureEnvironmentManager {
    private AllureEnvironmentManager() { }
    public static void setEnvironmentVariables() {
        try {
            Path resultDir = OutputPaths.allureResults();
            Files.createDirectories(resultDir);
            ExecutionPace pace = ExecutionPace.current();
            String environment =
                    "Browser="
                            + PropertyReader.getProperty("browserType")
                            + "\n"
                            + "Execution="
                            + PropertyReader.getProperty("executionType")
                            + "\n"
                            + "ExecutionPace="
                            + pace.name()
                            + "\n"
                            + "PaceTypingDelayMs="
                            + pace.minCharDelayMs()
                            + "-"
                            + pace.maxCharDelayMs()
                            + "\n"
                            + "PaceActionDelayMs="
                            + pace.minActionDelayMs()
                            + "-"
                            + pace.maxActionDelayMs()
                            + "\n"
                            + "PaceTransitionDelayMs="
                            + pace.minTransitionDelayMs()
                            + "-"
                            + pace.maxTransitionDelayMs()
                            + "\n"
                            + "System="
                            + PropertyReader.activeSystem()
                            + "\n"
                            + "OS="
                            + System.getProperty("os.name")
                            + "\n";
            Files.writeString(resultDir.resolve("environment.properties"), environment);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to prepare Allure environment", e);
        }
    }
}
