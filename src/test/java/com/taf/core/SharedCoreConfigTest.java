package com.taf.core;

import com.taf.core.utils.OutputPaths;
import com.taf.core.utils.dataReader.JsonReader;
import com.taf.core.utils.dataReader.LocalCredentials;
import com.taf.core.utils.dataReader.SystemProfile;
import com.taf.systems.zakaadash.config.ConfigManager;
import com.taf.systems.zulfi.config.ZulfiConfig;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/** Contracts of the shared configuration layer: -D precedence, namespaced profiles, fixture overrides, output roots. */
public class SharedCoreConfigTest {

    private static final String[] TOUCHED = {
            "zulfi.baseUrl", "baseUrlWeb", "zakaadash.baseUrl", "zakaadash.base.url", "output.dir",
            "allure.results.directory", "data.zakaadash.login-data.admin.emailEnv"};

    @AfterMethod(alwaysRun = true)
    public void clearProperties() {
        for (String key : TOUCHED) System.clearProperty(key);
    }

    @Test(description = "CORE-CFG-001 — Each system reads its own profile default")
    public void profilesAreNamespacedPerSystem() {
        Assert.assertEquals(ZulfiConfig.baseUrl(), "https://zulfi.zakaadash.com/zakaadashZulfi-portal-vue/");
        Assert.assertEquals(ConfigManager.config().zakaadashBaseUrl(), "https://app.dev.zakaadash.com/innova-vue");
    }

    @Test(description = "CORE-CFG-002 — JVM -D property overrides the profile default and is normalized")
    public void jvmPropertyOverridesProfile() {
        System.setProperty("zulfi.baseUrl", "https://zulfi.example.test/portal//");
        System.setProperty("zakaadash.baseUrl", "https://zd.example.test/app/");
        Assert.assertEquals(ZulfiConfig.baseUrl(), "https://zulfi.example.test/portal/");
        Assert.assertEquals(ConfigManager.config().zakaadashBaseUrl(), "https://zd.example.test/app");
    }

    @Test(description = "CORE-CFG-003 — Legacy keys still work when the new key is absent")
    public void legacyKeysRemainSupported() {
        System.setProperty("baseUrlWeb", "https://legacy.example.test/zulfi");
        System.setProperty("zakaadash.base.url", "https://legacy.example.test/zd");
        Assert.assertEquals(ZulfiConfig.baseUrl(), "https://legacy.example.test/zulfi/");
        Assert.assertEquals(ConfigManager.config().zakaadashBaseUrl(), "https://legacy.example.test/zd");
    }

    @Test(description = "CORE-CFG-004 — Missing or invalid settings name the system and key")
    public void missingSettingsProduceActionableErrors() {
        SystemProfile profile = new SystemProfile("zakaadash");
        IllegalStateException missing = Assert.expectThrows(IllegalStateException.class,
                () -> profile.require("zakaadash.not.configured"));
        Assert.assertTrue(missing.getMessage().contains("zakaadash.not.configured"));
        Assert.assertTrue(missing.getMessage().contains("'zakaadash'"));
        System.setProperty("zakaadash.baseUrl", "not-a-url");
        IllegalStateException invalid = Assert.expectThrows(IllegalStateException.class,
                () -> ConfigManager.config().zakaadashBaseUrl());
        Assert.assertTrue(invalid.getMessage().contains("zakaadash.baseUrl"));
    }

    @Test(description = "CORE-CFG-005 — URL join neither doubles nor drops slashes")
    public void joinNormalizesSlashes() {
        Assert.assertEquals(SystemProfile.join("https://a.test/app/", "/auth/login"), "https://a.test/app/auth/login");
        Assert.assertEquals(SystemProfile.join("https://a.test/app", "auth"), "https://a.test/app/auth");
    }

    @Test(description = "CORE-DATA-001 — Per-run -Ddata.<system>.<file>.<path> overrides fixture values")
    public void jsonReaderSupportsPerRunOverrides() {
        JsonReader reader = new JsonReader("zakaadash", "login-data");
        Assert.assertEquals(reader.getJsonData("admin.emailEnv"), "ZAKAADASH_EMAIL");
        System.setProperty("data.zakaadash.login-data.admin.emailEnv", "OVERRIDDEN");
        Assert.assertEquals(reader.getJsonData("admin.emailEnv"), "OVERRIDDEN");
        Assert.assertEquals(new JsonReader("zulfi", "login-data").getJsonData("user.passwordEnv"), "ZULFI_PASSWORD");
    }

    @Test(description = "CORE-CRED-001 — Credentials resolve from -D, then env, then the local file; blanks are ignored")
    public void credentialsResolveFromLocalFileWithPrecedence() throws Exception {
        Path file = Files.createTempFile("taf-credentials", ".properties");
        try {
            Files.writeString(file, "TAF_TEST_CRED_A=from-file\nTAF_TEST_CRED_B=  \nTAF_TEST_CRED_C=pa\\\\ss#word!\n");
            System.setProperty("credentials.file", file.toString());
            Assert.assertEquals(LocalCredentials.get("TAF_TEST_CRED_A"), "from-file");
            Assert.assertNull(LocalCredentials.get("TAF_TEST_CRED_B"), "blank value counts as not set");
            Assert.assertNull(LocalCredentials.get("TAF_TEST_CRED_MISSING"));
            Assert.assertEquals(LocalCredentials.get("TAF_TEST_CRED_C"), "pa\\ss#word!", "special characters survive");
            System.setProperty("TAF_TEST_CRED_A", "from-jvm");
            Assert.assertEquals(LocalCredentials.get("TAF_TEST_CRED_A"), "from-jvm", "-D wins over the file");
        } finally {
            System.clearProperty("TAF_TEST_CRED_A");
            System.clearProperty("credentials.file");
            Files.deleteIfExists(file);
        }
    }

    @Test(description = "CORE-CRED-002 — Missing local file is not an error and committed fixtures hold no secrets")
    public void missingFileIsFineAndFixturesContainNoSecrets() throws Exception {
        System.setProperty("credentials.file", "target/does-not-exist.properties");
        try {
            Assert.assertNull(LocalCredentials.get("TAF_TEST_CRED_NOPE"));
        } finally {
            System.clearProperty("credentials.file");
        }
        String[][] references = {
                {"zulfi", "user.usernameEnv"}, {"zulfi", "user.passwordEnv"},
                {"zakaadash", "admin.emailEnv"}, {"zakaadash", "admin.passwordEnv"}};
        for (String[] reference : references) {
            String value = new JsonReader(reference[0], "login-data").getJsonData(reference[1]);
            Assert.assertTrue(value.matches("[A-Z][A-Z0-9_]*"),
                    reference[0] + " login-data.json " + reference[1] + " must hold an environment variable NAME, not a value");
        }
    }

    @Test(description = "CORE-OUT-001 — output.dir and allure.results.directory isolate run artifacts")
    public void outputPathsFollowRunRoot() {
        System.setProperty("output.dir", "build-check/run-42");
        Path root = Path.of("build-check/run-42").toAbsolutePath().normalize();
        Assert.assertEquals(OutputPaths.screenshots(), root.resolve("screenshots"));
        Assert.assertEquals(OutputPaths.allureResults(), root.resolve("allure-results"));
        System.setProperty("allure.results.directory", "elsewhere/allure");
        Assert.assertEquals(OutputPaths.allureResults(), Path.of("elsewhere/allure").toAbsolutePath().normalize());
    }
}
