package com.znsio.api;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.EyesRunner;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.TestResultsStatus;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.TestResultContainer;
import com.applitools.eyes.selenium.ClassicRunner;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.znsio.api.utils.Config;
import io.appium.java_client.AppiumDriver;
import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.testng.xml.XmlTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.znsio.api.ApplitoolsConfigurationManager.config;

public class VisualTest {
    private static WebDriver webDriver;
    private static AppiumDriver appiumDriver;
    protected com.applitools.eyes.selenium.Eyes eyesOnWeb;
    protected com.applitools.eyes.appium.Eyes eyesOnApp;
    private static BatchInfo batch;
    private static Configuration eyesConfig;
    private static EyesRunner runner;
    private static final Logger LOGGER = Logger.getLogger(VisualTest.class.getName());

    public static void driverSetupForVisualTest(WebDriver wDriver) {
        webDriver = wDriver;
    }

    public static void driverSetupForVisualTest(AppiumDriver aDriver) {
        appiumDriver = aDriver;
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
        if (ApplitoolsConfigurationManager.isPlatformWeb() && ApplitoolsConfigurationManager.isUltraFastGridEnabled()) {
            ApplitoolsConfigurationManager.setUFGBrowserConfig(eyesConfig);
            runner = new VisualGridRunner(new RunnerOptions()
                    .testConcurrency(ApplitoolsConfigurationManager.getConcurrency()));
        } else {
            runner = new ClassicRunner();
        }
        runner.setDontCloseBatches(true);
    }

    @BeforeMethod
    public void initiateVisualTests(Method method) {
        LOGGER.info("@BeforeMethod of VisualTest called: " + method.getName());
        if (ApplitoolsConfigurationManager.isPlatformWeb()) {
            initiateVisualWebTests(method);
        } else {
            initiateVisualAppTests(method);
        }
    }

    @AfterMethod
    public void closeVisualTest(ITestResult iTestResult, ITestContext context) {
        LOGGER.info("@AfterMethod of VisualTest called: Waiting for visual validation results of test: " +
                iTestResult.getName());
        if (ApplitoolsConfigurationManager.isPlatformWeb()) {
            eyesOnWeb.closeAsync();
        } else {
            eyesOnApp.closeAsync();
        }
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

    private void initiateVisualWebTests(Method method) {
        eyesOnWeb = new com.applitools.eyes.selenium.Eyes(runner);
        if (ApplitoolsConfigurationManager.isLogsEnabled()) {
            eyesOnWeb.setLogHandler(new StdoutLogHandler(ApplitoolsConfigurationManager.isLogsEnabled()));
        }
        eyesOnWeb.setConfiguration(eyesConfig);
        boolean isTestPartOfVisualGroup = Arrays.toString(method.getAnnotation(Test.class).groups())
                .contains(ApplitoolsConfigurationManager.getVisualValidationGroupName());
        LOGGER.info("Is test part of '" +
                ApplitoolsConfigurationManager.getVisualValidationGroupName() + "' group: " + isTestPartOfVisualGroup);
        eyesOnWeb.setIsDisabled((!isTestPartOfVisualGroup || ApplitoolsConfigurationManager.isApplitoolsDisabled()));
        LOGGER.info("Is Applitools enabled for " + method.getName() + ": " + !eyesOnWeb.getIsDisabled());
        eyesOnWeb.open(webDriver,
                eyesConfig.getAppName() + "-" +
                        config.getProperty(Config.PLATFORM).toUpperCase() + "-" +
                        eyesConfig.getEnvironmentName(),
                method.getDeclaringClass().getSimpleName() + "-" + method.getName(),
                eyesConfig.getViewportSize());
    }

    private void initiateVisualAppTests(Method method) {
        eyesOnApp = new com.applitools.eyes.appium.Eyes(runner);
        if (ApplitoolsConfigurationManager.isLogsEnabled()) {
            eyesOnApp.setLogHandler(new StdoutLogHandler(ApplitoolsConfigurationManager.isLogsEnabled()));
        }
        eyesOnApp.setConfiguration(eyesConfig);
        boolean isTestPartOfVisualGroup = Arrays.toString(method.getAnnotation(Test.class).groups())
                .contains(ApplitoolsConfigurationManager.getVisualValidationGroupName());
        LOGGER.info("Is test part of '" +
                ApplitoolsConfigurationManager.getVisualValidationGroupName() + "' group: " + isTestPartOfVisualGroup);
        eyesOnApp.setIsDisabled((!isTestPartOfVisualGroup || ApplitoolsConfigurationManager.isApplitoolsDisabled()));
        LOGGER.info("Is Applitools enabled for " + method.getName() + ": " + !eyesOnApp.getIsDisabled());
        eyesOnApp.open(appiumDriver,
                eyesConfig.getAppName() + "-" +
                        config.getProperty(Config.PLATFORM).toUpperCase() + "-" +
                        eyesConfig.getEnvironmentName(),
                method.getDeclaringClass().getSimpleName() + "-" + method.getName());
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
            LOGGER.info("Visual validation results" + logMessage +
                    "\nResults are available here: " + result.getUrl());
        }
        boolean hasMismatches = result.getMismatches() != 0 || result.isAborted();
        LOGGER.info("Visual validation failed? - " + hasMismatches);
        return hasMismatches;
    }
}
