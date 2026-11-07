package com.taf.systems.zulfi.clubdata;

import com.taf.systems.zulfi.ui.PortalDom;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ordering for portal dates rendered as «السبت، 19 سبتمبر» (no year; the season runs July → June). */
public final class ArabicDates {

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("يناير", 1), Map.entry("فبراير", 2), Map.entry("مارس", 3), Map.entry("أبريل", 4),
            Map.entry("ابريل", 4), Map.entry("مايو", 5), Map.entry("يونيو", 6), Map.entry("يوليو", 7),
            Map.entry("أغسطس", 8), Map.entry("اغسطس", 8), Map.entry("سبتمبر", 9), Map.entry("أكتوبر", 10),
            Map.entry("اكتوبر", 10), Map.entry("نوفمبر", 11), Map.entry("ديسمبر", 12));

    private static final Pattern DAY_MONTH = Pattern.compile("(\\d{1,2})\\s+(\\p{InArabic}+)");

    private ArabicDates() {}

    /** Season-relative sort key (July = first month); {@link Integer#MAX_VALUE} when the date is unreadable. */
    public static int seasonOrderKey(String dateText) {
        Matcher m = DAY_MONTH.matcher(PortalDom.normalize(dateText));
        while (m.find()) {
            Integer month = MONTHS.get(m.group(2));
            if (month != null) {
                int seasonMonth = (month - 7 + 12) % 12;
                return seasonMonth * 100 + Integer.parseInt(m.group(1));
            }
        }
        return Integer.MAX_VALUE;
    }

    private static final Pattern DAY_MONTH_YEAR = Pattern.compile("(\\d{1,2})\\s+(\\p{InArabic}+)\\s+(\\d{4})");

    /** «٢٠ أكتوبر ٢٠٢٦ في ١٠:٠٤ ص» → 2026-10-20 (analysis cards); empty when no full Arabic date is present. */
    public static Optional<LocalDate> parseDayMonthYear(String text) {
        Matcher m = DAY_MONTH_YEAR.matcher(PortalDom.normalize(text));
        while (m.find()) {
            Integer month = MONTHS.get(m.group(2));
            if (month != null) {
                return Optional.of(LocalDate.of(
                        Integer.parseInt(m.group(3)), month, Integer.parseInt(m.group(1))));
            }
        }
        return Optional.empty();
    }

    /** «الأربعاء، ٢٨ أكتوبر» and «الأربعاء، 28 أكتوبر» compare equal. */
    public static boolean sameDisplayedDate(String a, String b) {
        return PortalDom.normalize(a).replace("،", ",").equals(PortalDom.normalize(b).replace("،", ","));
    }
}
