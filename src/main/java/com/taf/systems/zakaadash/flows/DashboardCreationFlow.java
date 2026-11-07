package com.taf.systems.zakaadash.flows;

import com.taf.systems.zakaadash.pages.SourceGroup;
import com.taf.systems.zakaadash.pages.dashboard.DashboardCreationModal;
import com.taf.systems.zakaadash.utils.BusinessStep;
import com.taf.systems.zakaadash.utils.ZakaaDashLog;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

public class DashboardCreationFlow {

    private static final String PAGE = "Dashboard Creation";

    private static final String MAIN_INDICATORS = """
            إجمالي الإيرادات باليورو، إجمالي التكاليف باليورو، صافي العائد المالي،
            العائد المالي على الاستثمار ROI، ومكونات الإيرادات من البث والإيرادات
            التجارية وإيرادات المباريات وبيع اللاعبين ومكافآت الرعاية. مقارنة الأندية
            والدوريات والدول والمواسم من حيث الربحية ونمو الإيرادات والتكاليف،
            مع عرض النقاط والانتصارات وفارق الأهداف ومتوسط الحضور كمؤشرات رياضية
            مساندة للتحليل المالي.""";

    private static final String GOALS = """
            إنشاء لوحة تحليلية لأصحاب المصلحة والإدارة المالية تقارن الأداء المالي
            للأندية عبر الدوريات والدول والمواسم المتاحة في البيانات. عرض بطاقات
            لإجمالي الإيرادات وإجمالي التكاليف وصافي العائد المالي والعائد المالي
            على الاستثمار، مع توضيح أن العملة هي اليورو.

            احتساب إجمالي الإيرادات من إيرادات البث والإيرادات التجارية وإيرادات
            المباريات وبيع اللاعبين ومكافآت الرعاية، وإجمالي التكاليف من الأجور
            وتكاليف التشغيل والإنفاق على انتقالات اللاعبين. احتساب صافي العائد
            بطرح التكاليف من الإيرادات، واحتساب ROI بقسمة صافي العائد على التكاليف
            وضرب الناتج في 100 مع معالجة حالة التكاليف الصفرية. عند تجميع عدة أندية
            أو مواسم، احتساب ROI من مجموع صافي العائد ومجموع التكاليف، دون جمع
            أو أخذ المتوسط البسيط لنسب ROI الفردية.

            مقارنة الإيرادات والتكاليف وصافي العائد بين الأندية والدوريات، وإظهار
            اتجاهاتها عبر المواسم ونسب تغيرها مقارنة بالموسم السابق عندما تتوفر
            بياناته، وتحديد الأندية الأعلى والأقل ربحية والأندية ذات العائد السلبي.
            توضيح مساهمة كل مصدر في الإيرادات ومساهمة كل بند في التكاليف.

            إتاحة تصفية النتائج بحسب الدوري والدولة والنادي والموسم، وإظهار
            النقاط والانتصارات وفارق الأهداف ومتوسط الحضور بجانب المؤشرات المالية
            لدراسة العلاقة بين الأداء الرياضي والعائد المالي دون افتراض علاقة
            سببية. استخدام الحقول الموجودة فقط، وتوضيح أن البيانات تجريبية
            اصطناعية وأن ROI هنا مقياس مالي مبسط للموسم وليس عائداً استثمارياً
            محاسبياً مدققاً.""";

    private final WebDriver driver;

    public DashboardCreationFlow(WebDriver driver) {
        this.driver = driver;
    }

    public DashboardCreationModal openFromProcessedVersion(SourceGroup sourceGroup, String processedVersionName) {
        BusinessStep.run(PAGE, "Click create dashboard on processed version row", () -> {
            sourceGroup.clickCreateDashboardForProcessedVersion(processedVersionName);
            ZakaaDashLog.info("Processed-version dashboard button — CLICKED");
        });
        DashboardCreationModal modal = new DashboardCreationModal(driver).waitUntilLoaded();
        BusinessStep.run(PAGE, "Verify processed version is dashboard data source", () -> {
            modal.verifyDataSourceIsProcessedVersion(processedVersionName);
            ZakaaDashLog.info("Dashboard data source — VERIFIED: " + processedVersionName);
        });
        return modal;
    }

    public void completeDataEntryStep(DashboardCreationModal modal, String dashboardName) {
        BusinessStep.run(PAGE, "Complete dashboard data entry step", () -> {
            modal.completeDataEntryStep(
                    dashboardName,
                    "لوحة تحليلية",
                    "المالية",
                    "محلل مالي",
                    MAIN_INDICATORS);
            ZakaaDashLog.info("Dashboard details — COMPLETED");
        });
    }

    public void completeGraphicsStep(DashboardCreationModal modal) {
        BusinessStep.run(PAGE, "Enter dashboard goals", () -> {
            modal.enterGoals(GOALS);
            ZakaaDashLog.info("Dashboard goals — ENTERED");
        });
        BusinessStep.run(PAGE, "Configure scope, charts, and advance to design step", () -> {
            modal.completeGraphicsAfterGoals();
        });
    }

    public void submitDesignAndWaitForRecommendedCharts(DashboardCreationModal modal) {
        BusinessStep.run(PAGE, "Submit design colors and wait for recommended charts", () -> {
            modal.submitDesignStepAndWaitForRecommendedCharts();
        });
    }

    public void completeRecommendedChartSelection(DashboardCreationModal modal, String dashboardName) {
        BusinessStep.run(PAGE, "Star one recommended chart and save dashboard", () -> {
            modal.completeRecommendedChartSelectionStage(dashboardName);
        });
    }

    public void closeDashboardWizard(DashboardCreationModal modal) {
        BusinessStep.run(PAGE, "Close dashboard wizard", () -> {
            if (!modal.isLoaded()) {
                return;
            }
            modal.closeWizardHandlingDiscard();
            Assert.assertFalse(modal.isLoaded(), "Dashboard creation modal should close");
        });
    }
}
