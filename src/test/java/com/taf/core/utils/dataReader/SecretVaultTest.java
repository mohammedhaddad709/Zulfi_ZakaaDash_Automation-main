package com.taf.core.utils.dataReader;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/** Contract of the Java side of runner/vault.py: same enc:v1: format, plain values unchanged, clear errors. */
public class SecretVaultTest {

    /** Produced by runner/vault.py (test-only key, not a real secret). */
    private static final String KEY = "FHt69O9hGJI2BD+aUrW2Bwxoqis7Xp6DNebfRY+LHwI=";
    private static final String VALUE = "enc:v1:FHFF6UICKsFOfLr8Xop4A9ymOECfcIAE8TMqZKL1Jv0fZXqq0NVfdDxOIL5VXg==";
    private static final String PLAIN = "Pässw0rd-زلفي";

    @AfterMethod(alwaysRun = true)
    public void clear() {
        System.clearProperty(SecretVault.KEY_NAME);
        System.clearProperty("VAULT_TEST_SECRET");
    }

    @Test(description = "CORE-VLT-001 — A value encrypted by runner/vault.py decrypts to the same text")
    public void decryptsPythonVaultFormat() {
        Assert.assertEquals(SecretVault.decrypt(VALUE, KEY), PLAIN);
    }

    @Test(description = "CORE-VLT-002 — Plain values pass through unchanged")
    public void plainValuesUnchanged() {
        Assert.assertEquals(SecretVault.reveal("plain-value"), "plain-value");
        Assert.assertNull(SecretVault.reveal(null));
    }

    @Test(description = "CORE-VLT-003 — Wrong key, missing key and damaged values fail with an actionable message")
    public void wrongOrMissingKeyFails() {
        IllegalStateException wrong = Assert.expectThrows(IllegalStateException.class,
                () -> SecretVault.decrypt(VALUE, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="));
        Assert.assertTrue(wrong.getMessage().contains(SecretVault.KEY_NAME));
        Assert.assertFalse(wrong.getMessage().contains(PLAIN));
        Assert.expectThrows(IllegalStateException.class, () -> SecretVault.decrypt(VALUE, ""));
        Assert.expectThrows(IllegalStateException.class, () -> SecretVault.decrypt("enc:v1:@@@", KEY));
        Assert.expectThrows(IllegalStateException.class, () -> SecretVault.decrypt("enc:v1:AAAA", KEY));
    }

    @Test(description = "CORE-VLT-004 — LocalCredentials decrypts an enc:v1: value with RUNNER_SECRET_KEY")
    public void localCredentialsDecrypt() {
        System.setProperty(SecretVault.KEY_NAME, KEY);
        System.setProperty("VAULT_TEST_SECRET", VALUE);
        Assert.assertEquals(LocalCredentials.get("VAULT_TEST_SECRET"), PLAIN);
    }
}
