package com.taf.systems.zulfi.analysis.identity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only extraction of numeric identity fields embedded in Vue/HTML page source. */
public final class AnalysisPageSourceIdentityScanner {

    private static final List<KeyPattern> KEY_VALUE_PATTERNS =
            List.of(
                    keyPattern("externalEventId"),
                    keyPattern("officialMatchId"),
                    keyPattern("matchId"),
                    keyPattern("eventId"));

    private static final Pattern API_URL_HINT =
            Pattern.compile(
                    "(https?://[^\"'\\s<>]+/(?:api|rest)[^\"'\\s<>]*analysis[^\"'\\s<>]*)",
                    Pattern.CASE_INSENSITIVE);

    private AnalysisPageSourceIdentityScanner() {}

    public static AnalysisPageSourceIdentityScan scan(String pageSource, String expectedExternalEventId) {
        if (pageSource == null) {
            pageSource = "";
        }
        Map<String, List<String>> hits = new LinkedHashMap<>();
        for (KeyPattern kp : KEY_VALUE_PATTERNS) {
            Matcher m = kp.pattern().matcher(pageSource);
            while (m.find()) {
                String value = m.group(1);
                hits.computeIfAbsent(kp.canonicalKey(), k -> new ArrayList<>()).add(value);
            }
        }
        List<String> apiHints = new ArrayList<>();
        Matcher api = API_URL_HINT.matcher(pageSource);
        while (api.find()) {
            String url = api.group(1);
            if (!apiHints.contains(url)) {
                apiHints.add(url);
            }
        }
        boolean containsExpectedExt =
                expectedExternalEventId != null
                        && !expectedExternalEventId.isBlank()
                        && pageSource.contains(expectedExternalEventId.trim());
        return new AnalysisPageSourceIdentityScan(hits, apiHints, containsExpectedExt);
    }

    private static KeyPattern keyPattern(String canonicalKey) {
        String snake = canonicalKey.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        Pattern pattern =
                Pattern.compile(
                        "(?:\""
                                + canonicalKey
                                + "\"|\""
                                + snake
                                + "\")\\s*[:=]\\s*\"?(\\d+)\"?",
                        Pattern.CASE_INSENSITIVE);
        return new KeyPattern(canonicalKey, pattern);
    }

    private record KeyPattern(String canonicalKey, Pattern pattern) {}

    public record AnalysisPageSourceIdentityScan(
            Map<String, List<String>> keyValues,
            List<String> apiUrlHints,
            boolean pageSourceContainsExpectedExternalEventId) {

        public String firstValue(String key) {
            List<String> values = keyValues.get(key);
            if (values == null || values.isEmpty()) {
                return "";
            }
            return values.getFirst();
        }

        public String summarize() {
            return "keyValues="
                    + keyValues
                    + "; apiHints="
                    + apiUrlHints
                    + "; containsExpectedExternalEventId="
                    + pageSourceContainsExpectedExternalEventId;
        }
    }
}
