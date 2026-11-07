package com.taf.systems.zakaadash.utils;

import org.testng.Reporter;

public final class ZakaaDashLog {

    private ZakaaDashLog() {
    }

    public static void info(String message) {
        String line = "[ZakaDash] " + message;
        System.out.println(line);
        Reporter.log(line, true);
    }
}
