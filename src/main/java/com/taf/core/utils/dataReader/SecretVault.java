package com.taf.core.utils.dataReader;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Decrypts {@code enc:v1:} values written by {@code runner/vault.py}: base64(12-byte nonce + ciphertext + 16-byte tag),
 * AES-256-GCM, key = base64 of 32 bytes in {@code RUNNER_SECRET_KEY} (JVM property or environment variable).
 *
 * <p>Plain values pass through unchanged. The key and the decrypted value are never logged.
 */
public final class SecretVault {

    public static final String PREFIX = "enc:v1:";
    public static final String KEY_NAME = "RUNNER_SECRET_KEY";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private SecretVault() { }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** Plain text of an {@code enc:v1:} value with the configured key; any other value is returned as is. */
    public static String reveal(String value) {
        if (!isEncrypted(value)) return value;
        return decrypt(value, configuredKey());
    }

    static String decrypt(String value, String base64Key) {
        if (base64Key == null || base64Key.isBlank())
            throw new IllegalStateException("Encrypted credential found but " + KEY_NAME + " is not set");
        byte[] key = decode(base64Key.strip(), KEY_NAME);
        if (key.length != 32) throw new IllegalStateException(KEY_NAME + " must decode to 32 bytes");
        byte[] blob = decode(value.substring(PREFIX.length()), "encrypted value");
        if (blob.length < NONCE_BYTES + TAG_BITS / 8) throw new IllegalStateException("Encrypted value is too short");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(blob, 0, NONCE_BYTES)));
            byte[] plain = cipher.doFinal(blob, NONCE_BYTES, blob.length - NONCE_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot decrypt credential: wrong " + KEY_NAME + " or damaged value");
        }
    }

    private static String configuredKey() {
        String fromProperty = System.getProperty(KEY_NAME);
        return fromProperty != null && !fromProperty.isBlank() ? fromProperty : System.getenv(KEY_NAME);
    }

    private static byte[] decode(String text, String what) {
        try {
            return Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(what + " is not valid base64");
        }
    }
}
