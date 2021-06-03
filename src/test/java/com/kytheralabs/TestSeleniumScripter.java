package com.kytheralabs;

import org.junit.*;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.remote.RemoteWebDriver;


public class TestSeleniumScripter {
    private final String driverType = "firefox";
    private final Yaml yamlParser = new Yaml();
    private final FirefoxOptions driverOptions = new FirefoxOptions();
    private final List<String> options = Arrays.asList("--no-sandbox",
                                                       "--ignore-certificate-errors",
//                                                       "--headless",
                                                       "--window-size=1920,1080",
                                                       "--start-maximized",
                                                       "--disable-gpu",
                                                       "--disable-extensions",
                                                       "--disable-infobars");

    private RemoteWebDriver driver = null;

    @Before
    public void setUp() {
        options.forEach(driverOptions::addArguments);
        driver = new FirefoxDriver(driverOptions);
    }

    @After
    public void tearDown() {
        driver.close();
        driver = null;
    }

    private Map<String, Object> loadScript(String fileName) {
        InputStream inputStream = this.getClass()
                                      .getClassLoader()
                                      .getResourceAsStream(fileName);
        return yamlParser.load(inputStream);
    }

    private void runScript(String url, String scriptName) throws Exception {
        final Map<String, Object> script = loadScript(scriptName);
        driver.get(url);
        SeleniumScripter scriptRunner = new SeleniumScripter(driver);
        scriptRunner.runScript(script, null, null);
    }

    @Ignore
    @Test
    public void testAlabama() throws Exception {
        // Crawl parameters
        final String scriptName = "alabama.yaml";
        final String url = "https://www.medicaid.alabamaservices.org/ALPortal/NDC%20Look%20Up/tabId/39/Default.aspx";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void testBcbsms() throws Exception {
        // Crawl parameters
        final String scriptName = "bcbsms.yaml";
        final String url = "https://www.bcbsms.com/BlueLand/rx/rxDirectFormularyDrugSearch.do?year=2017&dotcom=true";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Ignore
    @Test
    public void testForward() throws Exception {
        // Crawl parameters
        final String scriptName = "forward.yaml";
        final String url = "https://www.forwardhealth.wi.gov/WIPortal/Subsystem/Provider/DrugSearch.aspx";

        // Start the crawl
        runScript(url, scriptName);
    }
}
