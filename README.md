# Introduction

1. Utility to easily integrate Applitools VisualAI with your JAVA-TestNG automation framework
2. This is a library which implements basic utilities
   for [Applitools](https://applitools.com/docs/topics/overview.html) `(com.applitools)`
3. Dashboard link: https://eyes.applitools.com/

# Build

`mvn clean install -DskipTests`
> If facing issues with dependencies not being resolved from https://jitpack.io, then check the `settings.xml` file
> you're using for building your maven projects. If you've proxies configured in the same, then make sure `jitpack.io`
> is part of `nonProxyHosts` configuration. For instance

```xml

<proxy>
    <id>httpmyproxy</id>
    <active>true</active>
    <protocol>http</protocol>
    <host>someHost</host>
    <port>8080</port>
    <username>UserName</username>
    <password>Password</password>
    <nonProxyHosts>*.google.com|*jitpack.io</nonProxyHosts>
</proxy>
```

# How to consume the dependency

   ```
   <dependency>
      <groupId>com.github.znsio</groupId>
      <artifactId>ApplitoolsIntegration</artifactId>
      <version>x.x.x</version>
   </dependency>
   ```

# How to configure Applitools (Visual Testing) in your automation framework

1. To disable visual validation for the tests, set `IS_VISUAL` to `false` in config properties of your automation
   framework repo
2. For running visual validations:
    1. Set `IS_VISUAL` to `true` in config properties of your automation framework repo
    2. Set the environment variable for `APPLITOOLS_API_KEY` like this
       ```
       export APPLITOOLS_API_KEY=<Your API KEY>
       ```
       Applitools API Key can be picked from user Dashboard `https://eyes.applitools.com/`
    3. **Properties file:** Create a `applitools.properties` file either in `src/test/resources` directory in your
       automation framework. Specify the path of the file in you `config.properties` of your automation framework like
       this:
       ```
       APPLITOOLS_CONFIGURATION_FILE=./src/test/resources/applitools.properties
       ```
       The `applitools.properties` file should have the following mandatory attributes:
       ```
       SERVER_URL=https://eyes.applitools.com/
       CONCURRENCY=<Integer Value>
       MATCH_LEVEL=strict
       SEND_DOM=<true or false>
       STITCH_MODE=css
       TAKE_FULL_PAGE_SCREENSHOT=<true or false>
       VIEWPORT_SIZE=1200x700
       USE_UFG=<true or false>
       SHOW_LOGS=<true or false>
       SAVE_BASELINE_FOR_NEW_TESTS=<true or false>
       ```
    4. Annotate the test with the `visual` TestNG group annotation which you want to run as part of visual validation.
       If the test is not part of `visual` group, visual validations will not take place for that test even
       if `IS_DISABLED` value is set to `false` and test contains the `eyes` validations. For instance,
       ```java
       @Test(description = "Validating login with valid username and password", groups = {"visual"})
       public void validLoginTest(String username, String password, String expectedMessage) throws InterruptedException {

          performLogin(username, password);
          verifyMessageAfterLogin(expectedMessage);
       }
       ```
    5. To perform Visual Validations on a test, add any of the following commands in the test wherever you need to run
       visual validation. Use one of the following approaches:
        1. #### Full Screen/Visible Viewport validation (Eyes.checkWindow()):
           The eyes check window command captures an image of all the content in the browser window. When capturing on a
           local browser/App, If the content in the window is larger than the viewport, then the command captures
           multiple images at different offsets in the window and stitches them together to obtain an image that
           includes all the content in the window.
           ```
           eyes.checkWindow("<A string message giving info about which screen you're validating>");
           ```
           For Instance:
           ```
           eyes.checkWindow("HomePage");
           ```
        2. #### Fluent API Check (Eyes.check()):
           In the Fluent API you create and configure checkpoints using the eyes$check method. You pass this method a
           parameter that is created from a chain of methods calls from the target and checksettings classes. The
           particular chain of methods you call, determines the target that will be checked and how it is configured.
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
            2. For more detailed explanation and different examples, visit
               URL: [The Eyes SDK check Fluent API](https://applitools.com/docs/topics/sdk/the-eyes-sdk-check-fluent-api.html)
    6. #### Using Ultra Fast Grid:
       Applitools Ultrafast Grid allows you to perform visual testing across multiple browsers and devices in seconds,
       to make sure that your content is visually perfect on every device, screen size, and browser combination. To Run
       Visual Validations on multiple Browser Types Using Ultra Fast Grid, Set `USE_UFG` to `true`
       in `applitools.properties` file of your automation framework repo. Browser Combination need to be added
       in `setUFGBrowserConfig(Configuration eyesConfig)`
       in `src/main/java/com/znsio/api/ApplitoolsConfigurationManager.java` as illustrated in Below example:
        ```
        eyesConfig.addBrowser(1512, 866, BrowserType.CHROME);
        eyesConfig.addBrowser(1600, 1200, BrowserType.FIREFOX);
        eyesConfig.addBrowser(1024, 768, BrowserType.SAFARI);
        eyesConfig.addDeviceEmulation(DeviceName.Galaxy_Note_10);
        eyesConfig.addDeviceEmulation(DeviceName.iPhone_11_Pro_Max);
        ```
       For More Details on Configuring Ultra Fast Grid,
       Visit: [Ultrafast Grid configuration](https://applitools.com/docs/topics/sdk/vg-configuration.html)
    7. #### Setting correct Viewport Size:
        1. Incorrect Viewport size will cause the tests to be skipped with exception
           as `EyesException: Unable to set Viewport Size`.
        2. In that case, follow the below process to set Viewport Size Correctly:
            - Navigate to Site: `https://whatismyviewport.com/` to get the viewport size.
            - Set the Viewport size in `VIEWPORT_SIZE` in format: `Widthxheight` in `applitools.properties` file of your
              automation framework repo

3. All the Applitools Batch, Config and UFG Browser configuration related code is present in file `
   src/main/java/com/znsio/api/ApplitoolsConfigurationManager.java``.
    1. To add Batch Properties, navigate to above java class and add code in
       method: `setBatchProperties(BatchInfo batchInfo)` as per given syntax and example:
       ```
       batchInfo.addProperty(<Key>, <Value>);
       ```
       example:
       ```
       batchInfo.addProperty("Agent Name", System.getenv("AGENT_NAME"));
       ```
    2. To add Test Properties, navigate to above Java class and add code in
       method `setTestProperties(Configuration eyesConfig)`as per given syntax and example:
        ```
       eyesConfig.setPropertyName(<Value>);
       ```
       example:
       ```
       eyesConfig.setAppName(rpProperties.getProperty(Config.APP_NAME));
       ```
    3. **Dynamic properties:** For setting any additional property to either batch/test properties which is not already
       configured in `ApplitoolsConfigurationManager` class, set that attribute key and value at either System Property
       level or at Environment variable. Any key with prefix `AP_BATCH_` (for Batch Property) or `AP_TEST_` (for Test
       property) set at System property or Environment variable level will be set as Applitools's Batch/Test property
       respectively. The method which takes care of this configuration is `setPropertiesFromSystemVariables` defined
       inside `ApplitoolsConfigurationManager` class.
       For instance, if you're setting environment variable like `export AP_BATCH_Version=0.0.1`
       and `export AP_TEST_Version=0.0.1`, then on Applitools dashboard, you'll see the corresponding properties
       as `Version:0.0.1` for Batch/Test property respectively
4. **Pipeline configuration:**
    1. Set an environment variable `IS_VISUAL` to `true` for configuring visual validations for
       pipeline executions. This property takes precedence over the `IS_VISUAL` property value of `config.properties` in
       your automation framework repo.
    2. The `ApplitoolsConfigurationManager` class takes care of setting the following batch/test properties in case the
       test execution is happening on CI (Pipeline)
       ```
       BUILD_ID
       AGENT_NAME
       BRANCH_NAME
       ```
       All these properties will be set as either batch/test properties if the execution is happening on CI, and if they
       are either set at System property or System environment variable. If your pipeline is using different keys to set
       the above attributes then the ones we're using above, for each such key, you can define the new key value in
       the `config.proprties` file of your automation framework like below:
       ```
       BUILD_ID=BUILD_BUILDID
       BRANCH_NAME=BUILD_SOURCEBRANCHNAME
       ```