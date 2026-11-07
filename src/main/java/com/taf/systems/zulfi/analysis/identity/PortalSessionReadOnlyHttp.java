package com.taf.systems.zulfi.analysis.identity;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Collectors;

/** Session cookie GET for read-only API probes (no mutations). */
public final class PortalSessionReadOnlyHttp {

    private PortalSessionReadOnlyHttp() {}

    public static HttpReadResult tryGetWithBrowserSession(WebDriver driver, String absoluteUrl) {
        if (absoluteUrl == null || absoluteUrl.isBlank()) {
            return HttpReadResult.failed("empty url");
        }
        try {
            String cookieHeader =
                    driver.manage().getCookies().stream()
                            .map(c -> cookieHeaderPair(c))
                            .collect(Collectors.joining("; "));
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(absoluteUrl.trim()))
                            .timeout(Duration.ofSeconds(25))
                            .header("Cookie", cookieHeader)
                            .header("Accept", "application/json, text/plain, */*")
                            .GET()
                            .build();
            HttpResponse<String> response =
                    HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(15))
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .build()
                            .send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpReadResult(
                    true,
                    response.statusCode(),
                    truncate(response.body(), 8000),
                    "");
        } catch (Exception ex) {
            return HttpReadResult.failed(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private static String cookieHeaderPair(Cookie cookie) {
        return cookie.getName() + "=" + cookie.getValue();
    }

    private static String truncate(String body, int max) {
        if (body == null) {
            return "";
        }
        return body.length() <= max ? body : body.substring(0, max) + "...[truncated]";
    }

    public record HttpReadResult(boolean attempted, int statusCode, String bodySnippet, String error) {

        static HttpReadResult failed(String message) {
            return new HttpReadResult(false, -1, "", message);
        }
    }
}
