package com.taf.systems.zulfi.flows.analysis;

import com.taf.systems.zulfi.analysis.fixture.IndexedFixtureEntry;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestEntry;
import com.taf.systems.zulfi.analysis.preflight.KnownFixtureExclusions;
import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureOptionSnapshot;
import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.reporting.DefectClassification;
import com.taf.systems.zulfi.ui.PrimeVueIndexedSelect;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** Non-mutating official fixture selection and autofill verification (no submit). */
public final class OfficialFixtureSelectionFlow {

    private final WebDriver driver;

    public OfficialFixtureSelectionFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Verify authenticated Home is ready")
    public HomePage verifyAuthenticatedHome() {
        HomePage home = new HomePage(driver).awaitReady();
        if (!home.isReady()) {
            throw new IllegalStateException("Home not ready after login");
        }
        return home;
    }

    @Step("Open Create Analysis dialog from Home")
    public CreateAnalysisPage openCreateAnalysisPopup() {
        verifyAuthenticatedHome().clickStartAnalysis();
        return new CreateAnalysisPage(driver).waitForCreatePopupReady();
    }

    @Step("Ensure match type is Official (رسمية)")
    public CreateAnalysisPage selectOfficialMatchType(CreateAnalysisPage form) {
        form.ensureMatchTypeOfficial();
        if (!form.isOfficialMatchSelectorVisible()) {
            throw new IllegalStateException("Official fixture selector not visible");
        }
        return form;
    }

    @Step("Select first enabled official fixture by dropdown label (user-like)")
    public CreateAnalysisPage selectFirstEnabledOfficialFixtureByLabel(CreateAnalysisPage form) {
        selectOfficialMatchType(form);
        form.openOfficialFixtureDropdown();
        String label =
                form.listEligibleOfficialFixtures().stream()
                        .filter(candidate -> !KnownFixtureExclusions.isKnownDuplicateFixture(candidate))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No enabled official fixture label available in dropdown"));
        form.selectOfficialFixtureByExactLabel(label);
        return form;
    }

    @Step("Fill optional metadata without submit")
    public CreateAnalysisPage fillOptionalMetadataReadOnly(CreateAnalysisPage form) {
        if (form.isDescriptionFieldVisible()) {
            form.enterOptionalDescription("Automation read-only fixture selection smoke");
        }
        if (form.isVideoUrlFieldVisible()) {
            form.enterVideoUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        }
        return form;
    }

    public DriftCheckResult verifyIndexMatchesManifest(
            CreateAnalysisPage form, OfficialFixtureManifestEntry manifestEntry) {
        form.openOfficialFixtureDropdown();
        IndexedFixtureEntry liveEntry =
                form.getOfficialFixtureOptionByIndex(manifestEntry.fixtureIndex());
        OfficialFixtureOptionSnapshot live = liveEntry.snapshot();
        boolean labelMatch =
                PrimeVueIndexedSelect.sameFixtureIdentity(
                        live,
                        new PrimeVueIndexedSelect.OfficialFixtureManifestIdentity(
                                manifestEntry.displayLabel(),
                                manifestEntry.matchId(),
                                manifestEntry.externalEventId()));
        if (!labelMatch) {
            return DriftCheckResult.drift(
                    manifestEntry,
                    live,
                    "Fixture order/identity drift at index "
                            + manifestEntry.fixtureIndex()
                            + "; manifest="
                            + manifestEntry.displayLabel()
                            + "; live="
                            + live.displayLabel());
        }
        return DriftCheckResult.stable(manifestEntry, liveEntry);
    }

    public record DriftCheckResult(
            boolean stable,
            OfficialFixtureManifestEntry manifestEntry,
            IndexedFixtureEntry liveEntry,
            String message,
            DefectClassification classification) {

        static DriftCheckResult stable(OfficialFixtureManifestEntry manifest, IndexedFixtureEntry live) {
            return new DriftCheckResult(true, manifest, live, "Index identity stable", null);
        }

        static DriftCheckResult drift(
                OfficialFixtureManifestEntry manifest,
                OfficialFixtureOptionSnapshot live,
                String message) {
            return new DriftCheckResult(
                    false,
                    manifest,
                    null,
                    message,
                    DefectClassification.ENVIRONMENT_ERROR);
        }
    }
}
