package com.znsio.api;

import com.znsio.rpi.utils.commandline.CommandLineExecutor;
import com.znsio.rpi.utils.commandline.CommandLineResponse;
import com.applitools.eyes.*;
import com.applitools.eyes.selenium.*;
import com.znsio.rpi.properties.Config;
import org.apache.log4j.Logger;
import org.testng.util.Strings;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

class ApplitoolsConfigurationManager {
    private static final Properties applitoolsProperties = new Properties();
    static final Properties rpProperties = Config.loadProperties(System.getProperty("CONFIG"));
    private static boolean isApplitoolsDisabled;
    private static final String VISUAL_VALIDATION_GROUP_NAME = "visual";
    private static final Logger LOGGER = Logger.getLogger(ApplitoolsConfigurationManager.class.getName());

    static boolean isApplitoolsDisabled() {
        return isApplitoolsDisabled;
    }

    static String getVisualValidationGroupName() {
        return VISUAL_VALIDATION_GROUP_NAME;
    }

    static void loadProperties() throws IOException {
        String applitoolsPropertiesFileName = rpProperties.getProperty(Config.APPLITOOLS_CONFIGURATION_FILE);
        applitoolsProperties.load(new FileInputStream(applitoolsPropertiesFileName));
        setApplitoolsStatus();
    }

    static void setBatchProperties(BatchInfo batchInfo) {
        batchInfo.addProperty("Operating System", System.getProperty("os.name"));
        batchInfo.addProperty("Operating System Version", System.getProperty("os.version"));
        if (Strings.isNotNullAndNotEmpty(System.getenv("BUILD_BUILDID"))) {
            batchInfo.addProperty("Run on Pipeline", "true");
            batchInfo.addProperty("Pipeline Execution Id", System.getenv("BUILD_BUILDID"));
            batchInfo.addProperty("Agent Name", System.getenv("AGENT_NAME"));
            batchInfo.addProperty("Branch Name", System.getenv("BUILD_SOURCEBRANCHNAME"));
        } else {
            batchInfo.addProperty("Run on Pipeline", "false");
            batchInfo.addProperty("Branch Name", getBranchNameUsingGitCommand());
        }
        batchInfo.addProperty("Platform", rpProperties.getProperty(Config.PLATFORM).toUpperCase());
        batchInfo.addProperty("Environment", rpProperties.getProperty(Config.TARGET_ENVIRONMENT).toUpperCase());
        batchInfo.addProperty("UFG Enabled", applitoolsProperties.getProperty("USE_UFG"));
    }

    static void setConfigProperties(Configuration eyesConfig) {
        eyesConfig.setAppName(rpProperties.getProperty(Config.APP_NAME));
        eyesConfig.setViewportSize(getViewportSize(applitoolsProperties.getProperty("VIEWPORT_SIZE")));
        eyesConfig.setApiKey(System.getenv("APPLITOOLS_API_KEY"));
        eyesConfig.setSendDom(Boolean.parseBoolean(applitoolsProperties.getProperty("SEND_DOM")));
        eyesConfig.setForceFullPageScreenshot(Boolean.parseBoolean(applitoolsProperties
                .getProperty("TAKE_FULL_PAGE_SCREENSHOT")));
        eyesConfig.setStitchMode(StitchMode.valueOf(applitoolsProperties.getProperty("STITCH_MODE").toUpperCase()));
        eyesConfig.setMatchLevel(MatchLevel.valueOf(applitoolsProperties.getProperty("MATCH_LEVEL").toUpperCase()));
        eyesConfig.setSaveNewTests(Boolean.parseBoolean(applitoolsProperties
                .getProperty("SAVE_BASELINE_FOR_NEW_TESTS")));
        eyesConfig.setEnvironmentName(rpProperties.getProperty(Config.TARGET_ENVIRONMENT).toUpperCase());
        eyesConfig.setServerUrl(applitoolsProperties.getProperty("SERVER_URL"));
        if (Strings.isNotNullAndNotEmpty(System.getenv("BUILD_BUILDID"))) {
            eyesConfig.setBranchName(System.getenv("BUILD_SOURCEBRANCHNAME"));
        } else {
            eyesConfig.setBranchName(getBranchNameUsingGitCommand());
        }
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
        isApplitoolsDisabled = !Boolean.parseBoolean(rpProperties.getProperty(Config.IS_VISUAL));
        LOGGER.info("Is Applitools enabled from Config file : " + !isApplitoolsDisabled);
        if (Strings.isNotNullAndNotEmpty(System.getenv(Config.IS_VISUAL))) {
            isApplitoolsDisabled = !Boolean.parseBoolean(System.getenv(Config.IS_VISUAL));
            LOGGER.info("Is Applitools enabled from \"IS_VISUAL\" environment variable: " + !isApplitoolsDisabled);
        }
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

    static void setProxyIfRequired(Configuration eyesConfig) {
        if (!isApplitoolsDisabled() && Strings.isNotNullAndNotEmpty(System.getenv("BUILD_BUILDID"))) {
            if (Strings.isNotNullAndNotEmpty(System.getProperty("HTTPS_PROXY"))) {
                eyesConfig.setProxy(new ProxySettings(System.getProperty("HTTPS_PROXY")));
                LOGGER.info("Configured Applitools with Proxy: " + System.getProperty("HTTPS_PROXY"));
            } else {
                throw new EyesException("ERROR: Unable to configure proxy settings for AppliTools. " +
                        "HTTPS_PROXY property value is null or empty");
            }
        }
    }
}
