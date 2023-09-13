package com.znsio.api;

import com.applitools.eyes.*;
import com.applitools.eyes.selenium.ClassicRunner;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.znsio.rpi.properties.Config;
import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.testng.xml.XmlTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.znsio.api.ApplitoolsConfigurationManager.rpProperties;
import static com.znsio.rpi.utils.ReportPortalLogger.logInfoMessage;

public class VisualTest {
    private static WebDriver driver;
    protected Eyes eyes;
    private static BatchInfo batch;
    private static Configuration eyesConfig;
    private static EyesRunner runner;
    private static final Logger LOGGER = Logger.getLogger(VisualTest.class.getName());

    public static void driverSetupForVisualTest(WebDriver webDriver) {
        driver = webDriver;
    }

    @BeforeSuite
    public void setUpVisualTests(XmlTest suite) throws IOException, RuntimeException {

        LOGGER.info("@BeforeSuite of VisualTest called");
        ApplitoolsConfigurationManager.loadProperties();
        eyesConfig = new Configuration();
        ApplitoolsConfigurationManager.setConfigProperties(eyesConfig);
        ApplitoolsConfigurationManager.setProxyIfRequired(eyesConfig);
        batch = new BatchInfo(eyesConfig.getAppName() + "-" + suite.getSuite().getName() + "-" + suite.getName());
        ApplitoolsConfigurationManager.setBatchProperties(batch);
        eyesConfig.setBatch(batch);
        if (ApplitoolsConfigurationManager.isUltraFastGridEnabled()) {
            ApplitoolsConfigurationManager.setUFGBrowserConfig(eyesConfig);
        }
        runner = ApplitoolsConfigurationManager.isUltraFastGridEnabled() ? new VisualGridRunner(new RunnerOptions()
                .testConcurrency(ApplitoolsConfigurationManager.getConcurrency())) : new ClassicRunner();
        runner.setDontCloseBatches(true);
    }

    @BeforeMethod
    public void initiateVisualTests(Method method) {
        LOGGER.info("@BeforeMethod of VisualTest called: " + method.getName());
        eyes = new Eyes(runner);
        if (ApplitoolsConfigurationManager.isLogsEnabled()) {
            eyes.setLogHandler(new StdoutLogHandler(ApplitoolsConfigurationManager.isLogsEnabled()));
        }
        eyes.setConfiguration(eyesConfig);
        boolean isTestPartOfVisualGroup = Arrays.toString(method.getAnnotation(Test.class).groups())
                .contains(ApplitoolsConfigurationManager.getVisualValidationGroupName());
        LOGGER.info("Is test part of '" +
                ApplitoolsConfigurationManager.getVisualValidationGroupName() + "' group: " + isTestPartOfVisualGroup);
        eyes.setIsDisabled((!isTestPartOfVisualGroup || ApplitoolsConfigurationManager.isApplitoolsDisabled()));
        LOGGER.info("Is Applitools enabled for " + method.getName() + ": " + !eyes.getIsDisabled());
        eyes.open(driver,
                eyesConfig.getAppName() + "-" +
                        rpProperties.getProperty(Config.PLATFORM).toUpperCase() + "-" +
                        eyesConfig.getEnvironmentName(),
                method.getDeclaringClass().getSimpleName() + "-" + method.getName(),
                eyesConfig.getViewportSize());
    }

    @AfterMethod
    public void closeVisualTest(ITestResult iTestResult, ITestContext context) {
        LOGGER.info("@AfterMethod of VisualTest called: Waiting for visual validation results of test: " +
                iTestResult.getName());
        eyes.closeAsync();
        TestResultsSummary allTestResults = runner.getAllTestResults(false);
        TestResultContainer[] results = allTestResults.getAllResults();
        LOGGER.info(String.format("Number of cross-browser tests run for test: %s: %d%n",
                iTestResult.getName(), results.length));
        boolean mismatchFound = false;
        for (TestResultContainer eachResult : results) {
            Throwable ex = results[0].getException();
            TestResults testResult = eachResult.getTestResults();
            mismatchFound = handleTestResults(testResult) || mismatchFound;
        }
        LOGGER.info(String.format("Overall Visual Validation failed? - %s%n", mismatchFound));
        if (mismatchFound) {
            String failingMessage = "Test: " + iTestResult.getName() + " has visual differences";
            LOGGER.info(failingMessage);
            //TODO - fail the test if there is a visual difference found\
        }
    }

    @AfterSuite
    public void closeBatch() {
        LOGGER.info("@AfterSuite of VisualTest called: Close Visual Test batch");
        batch.setCompleted(true);
    }

    private boolean handleTestResults(TestResults result) {
        if (!result.getStatus().equals(TestResultsStatus.Disabled)) {
            LOGGER.info("\tTest Name: " + result.getName() + " :: " + result);
            LOGGER.info("\tTest status: " + result.getStatus());
            String logMessage = String.format(
                    "\nTest Name = '%s'\nBrowser = %s\nOS = %s\nViewport = %dx%d\nCheckpoints Matched = %d" +
                            "\nCheckpoints Mismatched = %d\nCheckpoints Missing = %d\nTest Aborted? = %s\n",
                    result.getName(), result.getHostApp(), result.getHostOS(),
                    result.getHostDisplaySize().getWidth(), result.getHostDisplaySize().getHeight(),
                    result.getMatches(), result.getMismatches(), result.getMissing(),
                    (result.isAborted() ? "aborted" : "no"));
            logInfoMessage("Visual validation results" + logMessage +
                    "\nResults are available here: " + result.getUrl());
        }
        boolean hasMismatches = result.getMismatches() != 0 || result.isAborted();
        logInfoMessage("Visual validation failed? - " + hasMismatches);
        return hasMismatches;
    }
}
