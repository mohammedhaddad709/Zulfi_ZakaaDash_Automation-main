package com.taf.systems.zulfi.validations;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TeamFieldDisplayRulesTest {

    @Test
    public void resolvePrefersValueOverEmptyStatePlaceholder() {
        TeamFieldDisplayRules.ResolvedDisplay resolved =
                TeamFieldDisplayRules.resolveFromDomParts(
                        "الأخدود",
                        "أدخل الفريق المضيف",
                        "الأخدود",
                        "أدخل الفريق المضيف",
                        "");
        Assert.assertEquals(resolved.text(), "الأخدود");
        Assert.assertEquals(resolved.source(), "input.domProperty.value");
    }

    @Test
    public void emptyPromptAloneIsNotMeaningfulClubDisplay() {
        Assert.assertFalse(TeamFieldDisplayRules.isMeaningfulClubDisplay("أدخل الفريق المضيف"));
    }
}
