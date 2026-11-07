package com.taf.systems.zulfi.reporting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Resolves a Windows-compatible Allure 2 CLI without assuming allure.bat is on PATH. */
public final class AllureCliResolver {

    private AllureCliResolver() {}

    public static Optional<List<String>> resolveCommand() {
        List<String> candidates = new ArrayList<>();
        String fromProperty = System.getProperty("report.allureCli");
        if (fromProperty != null && !fromProperty.isBlank()) {
            candidates.add(fromProperty.trim());
        }
        String fromEnv = System.getenv("ALLURE_CLI");
        if (fromEnv != null && !fromEnv.isBlank()) {
            candidates.add(fromEnv.trim());
        }
        candidates.add("allure.bat");
        candidates.add("allure");
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            candidates.add(Path.of(appData, "npm", "allure.cmd").toString());
            candidates.add(Path.of(appData, "npm", "allure.bat").toString());
        }
        Path localTool =
                Path.of(System.getProperty("user.dir", "."), "tools", "allure-2.29.0", "bin", "allure.bat");
        if (Files.isRegularFile(localTool)) {
            candidates.add(0, localTool.toAbsolutePath().toString());
        }
        for (String candidate : candidates) {
            if (candidate.contains(" ") && !candidate.startsWith("\"")) {
                Path path = Path.of(candidate);
                if (Files.isRegularFile(path)) {
                    return Optional.of(List.of(path.toAbsolutePath().toString()));
                }
            }
            if (isRunnable(candidate)) {
                return Optional.of(List.of(candidate));
            }
        }
        return Optional.empty();
    }

    private static boolean isRunnable(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        if (Files.isRegularFile(Path.of(command))) {
            return true;
        }
        for (String dir : System.getenv("PATH").split(";")) {
            Path exe = Path.of(dir, command);
            if (Files.isRegularFile(exe)) {
                return true;
            }
            if (Files.isRegularFile(Path.of(dir, command + ".bat"))) {
                return true;
            }
            if (Files.isRegularFile(Path.of(dir, command + ".cmd"))) {
                return true;
            }
        }
        return false;
    }
}
