package com.znsio.api;

import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.ProxySettings;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.BrowserType;
import com.applitools.eyes.selenium.StitchMode;
import com.znsio.api.utils.Config;
import com.znsio.api.utils.commandline.CommandLineExecutor;
import com.znsio.api.utils.commandline.CommandLineResponse;
import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.config.Configuration;
import org.apache.log4j.Logger;
import org.testng.util.Strings;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.znsio.api.utils.OverriddenVariable.getOverriddenStringValue;

class ApplitoolsConfigurationManager {
    private static final Properties applitoolsProperties = new Properties();
    static final Properties config = Config.loadProperties(System.getProperty("CONFIG"));
    private static boolean isApplitoolsDisabled;
    private static final String VISUAL_VALIDATION_GROUP_NAME = "visual";
    private static final String NOT_SET = "not-set";
    private static final String BATCH_PROPERTY_PREFIX = "AP_BATCH_";
    private static final String TEST_PROPERTY_PREFIX = "AP_TEST_";
    private static final Logger LOGGER = Logger.getLogger(ApplitoolsConfigurationManager.class.getName());

    static boolean isApplitoolsDisabled() {
        return isApplitoolsDisabled;
    }

    static String getVisualValidationGroupName() {
        return VISUAL_VALIDATION_GROUP_NAME;
    }

    static void loadProperties() throws IOException {
        String applitoolsPropertiesFileName = config.getProperty(Config.APPLITOOLS_CONFIGURATION_FILE);
        applitoolsProperties.load(new FileInputStream(applitoolsPropertiesFileName));
        setApplitoolsStatus();
    }

    static void setBatchProperties(BatchInfo batchInfo) {
        batchInfo.addProperty("Operating System", System.getProperty("os.name"));
        batchInfo.addProperty("Operating System Version", System.getProperty("os.version"));
        if (isRunningInCI()) {
            batchInfo.addProperty("Run on Pipeline", "true");
            batchInfo.addProperty("Pipeline Execution ID", getOverriddenStringValue(Config.BUILD_ID,
                    getOverriddenStringValue(config.getProperty(Config.BUILD_ID), NOT_SET)));
            batchInfo.addProperty("Agent Name", getOverriddenStringValue(Config.AGENT_NAME,
                    getOverriddenStringValue(config.getProperty(Config.AGENT_NAME), NOT_SET)));
            batchInfo.addProperty("Branch Name", getOverriddenStringValue(Config.BRANCH_NAME,
                    getOverriddenStringValue(config.getProperty(Config.BRANCH_NAME), getBranchNameUsingGitCommand())));
        } else {
            batchInfo.addProperty("Run on Pipeline", "false");
            batchInfo.addProperty("Branch Name", getBranchNameUsingGitCommand());
        }
        if (!isPlatformWeb()) {
            batchInfo.addProperty("App Package", config.getProperty(Config.APP_PACKAGE_NAME));
            batchInfo.addProperty("Local Device Execution", config.getProperty(Config.IS_LOCAL_DEVICE));
        }
        batchInfo.addProperty("Platform", config.getProperty(Config.PLATFORM).toUpperCase());
        batchInfo.addProperty("Environment", config.getProperty(Config.TARGET_ENVIRONMENT).toUpperCase());
        batchInfo.addProperty("UFG Enabled", applitoolsProperties.getProperty("USE_UFG"));
        setPropertiesFromSystemVariables(batchInfo);
    }

    static void setTestProperties(Configuration eyesConfig) {
        eyesConfig.setAppName(config.getProperty(Config.APP_NAME));
        eyesConfig.setViewportSize(getViewportSize(applitoolsProperties.getProperty("VIEWPORT_SIZE")));
        eyesConfig.setApiKey(getOverriddenStringValue("APPLITOOLS_API_KEY"));
        eyesConfig.setSendDom(Boolean.parseBoolean(applitoolsProperties.getProperty("SEND_DOM")));
        eyesConfig.setForceFullPageScreenshot(Boolean.parseBoolean(applitoolsProperties
                .getProperty("TAKE_FULL_PAGE_SCREENSHOT")));
        eyesConfig.setStitchMode(StitchMode.valueOf(applitoolsProperties.getProperty("STITCH_MODE").toUpperCase()));
        eyesConfig.setMatchLevel(MatchLevel.valueOf(applitoolsProperties.getProperty("MATCH_LEVEL").toUpperCase()));
        eyesConfig.setSaveNewTests(Boolean.parseBoolean(applitoolsProperties
                .getProperty("SAVE_BASELINE_FOR_NEW_TESTS")));
        eyesConfig.setEnvironmentName(config.getProperty(Config.TARGET_ENVIRONMENT).toUpperCase());
        eyesConfig.setServerUrl(applitoolsProperties.getProperty("SERVER_URL"));
        if (isRunningInCI()) {
            eyesConfig.setBranchName(getOverriddenStringValue(Config.BRANCH_NAME,
                    getOverriddenStringValue(config.getProperty(Config.BRANCH_NAME), getBranchNameUsingGitCommand())));
        } else {
            eyesConfig.setBranchName(getBranchNameUsingGitCommand());
        }
        setPropertiesFromSystemVariables(eyesConfig);
    }

    static boolean isUltraFastGridEnabled() {
        return Boolean.parseBoolean(applitoolsProperties.getProperty("USE_UFG"));
    }

    static void setUFGBrowserConfig(Configuration eyesConfig) {
        eyesConfig.addBrowser(1512, 866, BrowserType.CHROME);
        eyesConfig.addBrowser(1600, 1200, BrowserType.FIREFOX);
        eyesConfig.addBrowser(1024, 768, BrowserType.SAFARI);
    }

    static int getConcurrency() {
        return Integer.parseInt(applitoolsProperties.getProperty("CONCURRENCY"));
    }

    static boolean isLogsEnabled() {
        return Boolean.parseBoolean(applitoolsProperties.getProperty("SHOW_LOGS"));
    }

    static void setApplitoolsStatus() {
        isApplitoolsDisabled = !Boolean.parseBoolean(config.getProperty(Config.IS_VISUAL));
        LOGGER.info("Is Applitools enabled from Config file : " + !isApplitoolsDisabled);
        if (Strings.isNotNullAndNotEmpty(getOverriddenStringValue(Config.IS_VISUAL))) {
            isApplitoolsDisabled = !Boolean.parseBoolean(getOverriddenStringValue(Config.IS_VISUAL));
            LOGGER.info("Is Applitools enabled from \"IS_VISUAL\" environment variable or System property: " + !isApplitoolsDisabled);
        }
    }

    static void setProxyIfAvailable(Configuration eyesConfig) {
        if (Strings.isNotNullAndNotEmpty(getProxyKey())) {
            eyesConfig.setProxy(new ProxySettings(getProxyKey()));
            LOGGER.info("Configured Applitools with Proxy: " + getProxyKey());
        } else {
            LOGGER.info("'PROXY_KEY' value not configured. Continuing with configuring proxy key for Applitools");
        }
    }

    static boolean isPlatformWeb() {
        return config.getProperty(Config.PLATFORM).equalsIgnoreCase("Web");
    }

    static boolean isRunningInCI() {
        return Boolean.parseBoolean(getOverriddenStringValue(Config.RUN_IN_CI, config.getProperty(Config.RUN_IN_CI)));
    }

    static String getProxyKey() {
        return getOverriddenStringValue(Config.PROXY_KEY,
                getOverriddenStringValue(config.getProperty(Config.PROXY_KEY)));
    }

    static boolean isFailTestWhenVisualDifferenceFound() {
        return Boolean.parseBoolean(applitoolsProperties.getProperty("FAIL_TEST_WHEN_DIFFERENCE_FOUND"));
    }

    private static String getBranchNameUsingGitCommand() {
        String[] getBranchNameCommand = new String[]{"git", "rev-parse", "--abbrev-ref", "HEAD"};
        CommandLineResponse response = CommandLineExecutor.execCommand(getBranchNameCommand);
        String branchName = response.getStdOut();
        LOGGER.info(String.format("Branch name from git command: '%s': '%s'",
                Arrays.toString(getBranchNameCommand), branchName));
        return branchName;
    }

    private static RectangleSize getViewportSize(String viewportSize) {
        try {
            String[] viewP = viewportSize.split("x");
            return new RectangleSize(Integer.parseInt(viewP[0]), Integer.parseInt(viewP[1]));
        } catch (NullPointerException e) {
            throw new RuntimeException("Unable to get viewport size from Applitools configuration",
                    e);
        }
    }

    private static void setPropertiesFromSystemVariables(BatchInfo batchInfo) {
        setPropertiesFromEnvVariables(batchInfo);
        setPropertiesFromSystemProperties(batchInfo);
    }

    private static void setPropertiesFromSystemVariables(Configuration eyesConfig) {
        setPropertiesFromEnvVariables(eyesConfig);
        setPropertiesFromSystemProperties(eyesConfig);
    }

    private static void setPropertiesFromSystemProperties(BatchInfo batchInfo) {
        Properties properties = System.getProperties();
        Set<String> set = properties.stringPropertyNames();
        for (String propkey : set) {
            if (propkey.startsWith(BATCH_PROPERTY_PREFIX)) {
                batchInfo.addProperty(getKeyWithoutPrefix(BATCH_PROPERTY_PREFIX, propkey),
                        properties.getProperty(propkey));
            }
        }
    }

    private static void setPropertiesFromSystemProperties(Configuration eyesConfig) {
        Properties properties = System.getProperties();
        Set<String> set = properties.stringPropertyNames();
        for (String propkey : set) {
            if (propkey.startsWith(TEST_PROPERTY_PREFIX)) {
                eyesConfig.addProperty(getKeyWithoutPrefix(TEST_PROPERTY_PREFIX, propkey),
                        properties.getProperty(propkey));
            }
        }
    }

    private static void setPropertiesFromEnvVariables(BatchInfo batchInfo) {
        Map<String, String> map = System.getenv();
        for (String envKey : map.keySet()) {
            if (envKey.startsWith(BATCH_PROPERTY_PREFIX)) {
                batchInfo.addProperty(getKeyWithoutPrefix(BATCH_PROPERTY_PREFIX, envKey),
                        map.get(envKey));
            }
        }
    }

    private static void setPropertiesFromEnvVariables(Configuration eyesConfig) {
        Map<String, String> map = System.getenv();
        for (String envKey : map.keySet()) {
            if (envKey.startsWith(TEST_PROPERTY_PREFIX)) {
                eyesConfig.addProperty(getKeyWithoutPrefix(TEST_PROPERTY_PREFIX, envKey),
                        map.get(envKey));
            }
        }
    }

    private static String getKeyWithoutPrefix(String prefix, String key) {
        return key.substring(prefix.length());
    }
}

