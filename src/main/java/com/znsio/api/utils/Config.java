package com.znsio.api.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    public static final String TARGET_ENVIRONMENT = "TARGET_ENVIRONMENT";
    public static final String PLATFORM = "PLATFORM";
    public static final String APP_NAME = "APP_NAME";
    public static final String IS_VISUAL = "IS_VISUAL";
    public static final String IS_LOCAL_DEVICE = "IS_LOCAL_DEVICE";
    public static final String APPLITOOLS_CONFIGURATION_FILE = "APPLITOOLS_CONFIGURATION_FILE";
    public static final String RUN_IN_CI = "RUN_IN_CI";
    public static final String PIPELINE_EXECUTION_ID = "PIPELINE_EXECUTION_ID";
    public static final String AGENT_NAME = "AGENT_NAME";
    public static final String BRANCH_NAME = "BRANCH_NAME";
    private static final Logger LOGGER = Logger.getLogger(Config.class.getName());

    @NotNull
    public static Properties loadProperties(String configFile) {
        final Properties properties;
        try (InputStream input = new FileInputStream(System.getProperty("user.dir") + File.separator + configFile)) {
            properties = new Properties();
            properties.load(input);
        } catch (IOException ex) {
            LOGGER.info("ERROR: Config file not found, or unable to read it: "
                    + configFile + "\n");
            LOGGER.debug(ExceptionUtils.getStackTrace(ex));
            throw new RuntimeException("ERROR: Config file not found, or unable to read it: "
                    + configFile + "\n", ex);
        }
        return properties;
    }
}
