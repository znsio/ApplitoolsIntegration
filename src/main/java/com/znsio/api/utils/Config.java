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
    public static final String IS_MOBILAB = "IS_MOBILAB";
    public static final String BROWSER = "BROWSER";
    public static final String APP_PACKAGE_NAME = "APP_PACKAGE_NAME";
    public static final String URL = "URL";
    public static final String PAGE_LOAD_TIME = "PAGE_LOAD_TIME";
    public static final String TEST_DATA_FILE = "TEST_DATA_FILE";
    public static final String APPLITOOLS_CONFIGURATION_FILE = "APPLITOOLS_CONFIGURATION_FILE";
    public static final String TEST_REPORT_DIRECTORY = "TEST_REPORT_DIRECTORY";
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
