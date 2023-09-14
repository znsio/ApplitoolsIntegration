# Introduction
1. Utility to easily integrate Applitools VisualAI with your JAVA-TestNG automation framework
2. This is a library which implements basic utilities for [Applitools](https://applitools.com/docs/topics/overview.html) `(com.applitools)`
3. Dashboard link: https://jioeyes.applitools.com/

# Build
`mvn clean install -DskipTests -s ./settings.xml`

# How to consume the dependency
   ```
   <dependency>
      <groupId>com.github.znsio</groupId>
      <artifactId>ApplitoolsIntegration</artifactId>
      <version>x.x.x</version>
   </dependency>
   ```

# How to configure Applitools (Visual Testing) in your automation framework
1. To disable visual validation for the tests, set `IS_VISUAL` to `false` in config properties of your automation framework repo
2. For running visual validations:
    1. Set `IS_VISUAL` to `true` in config properties of your automation framework repo
    2. Set the environment variable for `APPLITOOLS_API_KEY` like this
       ```
       export APPLITOOLS_API_KEY=<Your API KEY>
       ```
       Applitools API Key can be picked from user Dashboard `https://jioeyes.applitools.com/` for QECC team
    3. Annotate the test with the `visual` TestNG group annotation which you want to run as part of visual validation. If the test is not part of `visual` group, visual validations will not take place for that test even if `IS_DISABLED` value is set to `false` and test contains the `eyes` validations.

    4. To perform Visual Validations on a test, Add any of the following commands in the test wherever you need to run visual validation. Use one of the following approaches:
        1. #### Eyes.checkWindow():
           The eyes check window command captures an image of all the content in the browser window. When capturing on a local browser/App, If the content in the window is larger than the viewport, then the command captures multiple images at different offsets in the window and stitches them together to obtain an image that includes all the content in the window.
           ```
           eyes.checkWindow("<A string message giving info about which screen you're validating>");
           ```
           For Instance:
           ```
           eyes.checkWindow("HomePage");
           ```
        2. #### Fluent API Check (Eyes.check()):
           In the Fluent API you create and configure checkpoints using the eyes$check method. You pass this method a parameter that is created from a chain of methods calls from the target and checksettings classes. The particular chain of methods you call, determines the target that will be checked and how it is configured.
           ```
           eyes.check("<string message>", <constraints>);
           ```
            1. **For instance**
               ```
               eyes.check("Home Page", Target.window().strict()
               .layout(By.xpath("//div[@class='ReactVirtualized__Grid items']"),
                  By.id("breadcrumb-container"),
                  By.className("//div[@class='  breadcrumb-section']"))
               .ignore(By.xpath("//div[@class='component-layout']//div[@class" + "='img-animate'][1]"),
                  By.xpath("//div[@class='banner-components-runtime']/div")));
               ```
            2. For more detailed explanation and different examples, visit URL: [The Eyes SDK check Fluent API](https://applitools.com/docs/topics/sdk/the-eyes-sdk-check-fluent-api.html)
    5. #### Using Ultra Fast Grid:
       Applitools Ultrafast Grid allows you to perform visual testing across multiple browsers and devices in seconds, to make sure that your content is visually perfect on every device, screen size, and browser combination.  To Run Visual Validations on multiple Browser Types Using Ultra Fast Grid, Set `USE_UFG` to `true` in `applitools.properties` file of your automation framework repo.
       Browser Combination need to be added in `setUFGBrowserConfig(Configuration eyesConfig)` in `src/main/java/com/znsio/api/ApplitoolsConfigurationManager.java` as illustrated in Below example:
       ```
       eyesConfig.addBrowser(1512, 866, BrowserType.CHROME);
       eyesConfig.addBrowser(1600, 1200, BrowserType.FIREFOX);
       eyesConfig.addBrowser(1024, 768, BrowserType.SAFARI);
       eyesConfig.addDeviceEmulation(DeviceName.Galaxy_Note_10);
       eyesConfig.addDeviceEmulation(DeviceName.iPhone_11_Pro_Max);
       ```
       For More Details on Configuring Ultra Fast Grid, Visit: [Ultrafast Grid configuration](https://applitools.com/docs/topics/sdk/vg-configuration.html)
    6. #### Setting correct Viewport Size:
        1. Incorrect Viewport size will cause the tests to be skipped with exception as `EyesException: Unable to set Viewport Size`.
        2. In that case, follow the below process to set Viewport Size Correctly:
            - Navigate to Site: `https://whatismyviewport.com/` to get the viewport size.
            - Set the Viewport size in `VIEWPORT_SIZE` in format: `Widthxheight` in `applitools.properties` file of your automation framework repo

3. All the Applitools Batch, Config and UFG Browser configuration related code is present in file `src/main/java/com/znsio/api/ApplitoolsConfigurationManager.java``.
    1. To add Batch Properties, navigate to above java class and add code in method: `setBatchProperties(BatchInfo batchInfo)` as per given syntax and example:
       ```
       batchInfo.addProperty(<Key>, <Value>);
       ```
       example:
       ```
       batchInfo.addProperty("Agent Name", System.getenv("AGENT_NAME"));
       ```
    2. To add Test Configuration Properties, navigate to above Java class and add code in method `setConfigProperties(Configuration eyesConfig)`as per given syntax and example:
        ```
       eyesConfig.setPropertyName(<Value>);
       ```
       example:
       ```
       eyesConfig.setAppName(rpProperties.getProperty(Config.APP_NAME));
       ```
4. **Pipeline configuration:** Set an environment variable `IS_VISUAL` to `true` for configuring visual validations for pipeline executions. This property takes precedence over the `IS_VISUAL` property value of config properties in your automation framework repo