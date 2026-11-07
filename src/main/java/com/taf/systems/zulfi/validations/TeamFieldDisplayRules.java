package com.taf.systems.zulfi.validations;

import java.util.Set;

/** Resolves club names shown in Create Analysis Home/Away readonly PrimeVue inputs. */
public final class TeamFieldDisplayRules {

    private static final Set<String> KNOWN_EMPTY_PROMPTS =
            Set.of(
                    "أدخل الفريق المضيف",
                    "أدخل الفريق الضيف",
                    "اكتب وصفاً");

    private TeamFieldDisplayRules() {}

    public record ResolvedDisplay(String text, String source) {}

    /**
     * PrimeVue autofill sets the club on {@code input.value} while the static empty-state prompt
     * remains on {@code input.placeholder}. Value must win when both are present.
     */
    public static ResolvedDisplay resolveFromDomParts(
            String domPropertyValue,
            String domPropertyPlaceholder,
            String attributeValue,
            String attributePlaceholder,
            String inputTextContent) {
        if (isMeaningfulClubDisplay(domPropertyValue)) {
            return new ResolvedDisplay(domPropertyValue.trim(), "input.domProperty.value");
        }
        if (isMeaningfulClubDisplay(domPropertyPlaceholder)) {
            return new ResolvedDisplay(domPropertyPlaceholder.trim(), "input.domProperty.placeholder");
        }
        if (isMeaningfulClubDisplay(attributeValue)) {
            return new ResolvedDisplay(attributeValue.trim(), "input.attribute.value");
        }
        if (isMeaningfulClubDisplay(attributePlaceholder)) {
            return new ResolvedDisplay(attributePlaceholder.trim(), "input.attribute.placeholder");
        }
        if (isMeaningfulClubDisplay(inputTextContent)) {
            return new ResolvedDisplay(inputTextContent.trim(), "input.textContent");
        }
        return new ResolvedDisplay("", "none");
    }

    public static boolean isMeaningfulClubDisplay(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String trimmed = text.trim();
        if (isEmptyFieldPrompt(trimmed)) {
            return false;
        }
        return !trimmed.equals("—") && !trimmed.equals("-");
    }

    public static boolean isEmptyFieldPrompt(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String trimmed = text.trim();
        if (KNOWN_EMPTY_PROMPTS.contains(trimmed)) {
            return true;
        }
        return trimmed.startsWith("أدخل ") || trimmed.startsWith("اختر ");
    }
}
