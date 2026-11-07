package com.taf.systems.zakaadash.tests.config;

import com.taf.systems.zakaadash.config.ZakaaDashCredentials;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ZakaaDashCredentialsTest {

    @Test
    public void credentialsAreConfiguredWithoutExposure() {
        ZakaaDashCredentials credentials = ZakaaDashCredentials.load();

        Assert.assertFalse(credentials.email().isBlank(), "Expected ZakaaDash email to be configured");
        Assert.assertFalse(credentials.password().isBlank(), "Expected ZakaaDash password to be configured");

        String rendered = credentials.toString();
        Assert.assertTrue(rendered.contains("***configured***"), "Expected masked email in toString()");
        Assert.assertTrue(rendered.contains("password='***'"), "Expected masked password in toString()");
        Assert.assertFalse(rendered.contains(credentials.email()), "toString() must not expose raw email");
        Assert.assertFalse(rendered.contains(credentials.password()), "toString() must not expose raw password");
    }
}
