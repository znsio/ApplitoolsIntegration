package com.znsio.api;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.EyesRunner;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.TestResultsStatus;
import com.applitools.eyes.selenium.ClassicRunner;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.znsio.api.utils.Config;
import io.appium.java_client.AppiumDriver;
import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.testng.xml.XmlTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.znsio.api.ApplitoolsConfigurationManager.*;

public class ApplitoolsInitializer {
    private static WebDriver webDriver;
    private static AppiumDriver appiumDriver;
    protected com.applitools.eyes.selenium.Eyes eyesOnWeb;
    protected com.applitools.eyes.appium.Eyes eyesOnApp;
    private static BatchInfo batch;
    private static Configuration eyesConfig;
    private static EyesRunner runner;
    private static final Logger LOGGER = Logger.getLogger(ApplitoolsInitializer.class.getName());

    public static void driverSetupForApplitoolsInitializer(WebDriver wDriver) {
        webDriver = wDriver;
    }

    public static void driverSetupForApplitoolsInitializer(AppiumDriver aDriver) {
        appiumDriver = aDriver;
    }

    @BeforeSuite
    public void setUpApplitoolsInitializer(XmlTest suite) throws IOException, RuntimeException {

        LOGGER.info("@BeforeSuite of ApplitoolsInitializer called");
        loadProperties();
        eyesConfig = new Configuration();
        setTestProperties(eyesConfig);
        setProxyIfAvailable(eyesConfig);
        batch = new BatchInfo(eyesConfig.getAppName() + "-" + suite.getSuite().getName() + "-" + suite.getName());
        setBatchProperties(batch);
        eyesConfig.setBatch(batch);
        if (isPlatformWeb() && isUltraFastGridEnabled()) {
            setUFGBrowserConfig(eyesConfig);
            runner = new VisualGridRunner(new RunnerOptions().testConcurrency(getConcurrency()));
        } else {
            runner = new ClassicRunner();
        }
        runner.setDontCloseBatches(true);
    }

    @BeforeMethod
    public void initiateApplitoolsInitializer(Method method) {
        LOGGER.info("@BeforeMethod of ApplitoolsInitializer called: " + method.getName());
        if (isPlatformWeb()) {
            initiateVisualWebTests(method);
        } else {
            initiateVisualAppTests(method);
        }
    }

    @AfterMethod
    public void closeApplitoolsInitializer(ITestResult iTestResult) {
        LOGGER.info("@AfterMethod of ApplitoolsInitializer called: Waiting for visual validation results of test: " +
                iTestResult.getName());
        TestResults testResult;
        if (isPlatformWeb()) {
            testResult = eyesOnWeb.close(false);
        } else {
            testResult = eyesOnApp.close(false);
        }
        boolean mismatchFound = handleVisualTestResults(testResult);
        LOGGER.info(String.format("Overall Visual Validation failed? - %s%n", mismatchFound));
        handleFunctionalTestResults(mismatchFound, iTestResult.getName(), testResult);
    }

    @AfterSuite
    public void closeBatch() {
        LOGGER.info("@AfterSuite of ApplitoolsInitializer called: Close Visual Test batch");
        batch.setCompleted(true);
    }

    private void initiateVisualWebTests(Method method) {
        eyesOnWeb = new com.applitools.eyes.selenium.Eyes(runner);
        if (isLogsEnabled()) {
            eyesOnWeb.setLogHandler(new StdoutLogHandler(isLogsEnabled()));
        }
        eyesOnWeb.setConfiguration(eyesConfig);
        boolean isTestPartOfVisualGroup = Arrays.toString(method.getAnnotation(Test.class).groups())
                .contains(getVisualValidationGroupName());
        LOGGER.info("Is test part of '" + getVisualValidationGroupName() + "' group: " + isTestPartOfVisualGroup);
        eyesOnWeb.setIsDisabled((!isTestPartOfVisualGroup || isApplitoolsDisabled()));
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
        if (isLogsEnabled()) {
            eyesOnApp.setLogHandler(new StdoutLogHandler(isLogsEnabled()));
        }
        eyesOnApp.setConfiguration(eyesConfig);
        boolean isTestPartOfVisualGroup = Arrays.toString(method.getAnnotation(Test.class).groups())
                .contains(getVisualValidationGroupName());
        LOGGER.info("Is test part of '" + getVisualValidationGroupName() + "' group: " + isTestPartOfVisualGroup);
        eyesOnApp.setIsDisabled((!isTestPartOfVisualGroup || isApplitoolsDisabled()));
        LOGGER.info("Is Applitools enabled for " + method.getName() + ": " + !eyesOnApp.getIsDisabled());
        eyesOnApp.open(appiumDriver,
                eyesConfig.getAppName() + "-" +
                        config.getProperty(Config.PLATFORM).toUpperCase() + "-" +
                        eyesConfig.getEnvironmentName(),
                method.getDeclaringClass().getSimpleName() + "-" + method.getName());
    }

    private boolean handleVisualTestResults(TestResults result) {
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

    private void handleFunctionalTestResults(boolean mismatchFound, String methodName, TestResults testResult) {
        if (mismatchFound) {
            String failingMessage = String.format("Test: '%s' has visual differences." +
                    "\nCheck the visual validation results here: '%s'", methodName, testResult.getUrl());
            LOGGER.info(failingMessage);
            if (isFailTestWhenVisualDifferenceFound()) {
                Assert.fail(failingMessage, new Throwable(String.format("'%s' marked as failed in @AfterMethod " +
                        "because visual validation failed", methodName)));
            }
        }
    }
}
