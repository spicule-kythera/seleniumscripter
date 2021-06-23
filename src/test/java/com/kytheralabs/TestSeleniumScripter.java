package com.kytheralabs;

import org.junit.*;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.io.InputStream;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.remote.RemoteWebDriver;


public class TestSeleniumScripter {
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

    private static Map<String, Object> loadScript(String fileName) {
        InputStream inputStream = TestSeleniumScripter.class
                                                      .getClassLoader()
                                                      .getResourceAsStream(fileName);
        return new Yaml().load(inputStream);
    }

    private void runScript(String url, String scriptName) throws Exception {
        final Map<String, Object> script = loadScript(scriptName);
        driver.get(url);
        SeleniumScripter scriptRunner = new SeleniumScripter(driver);
        scriptRunner.runScript(script);
    }

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

    @Test
    public void testForward() throws Exception {
        // Crawl parameters
        final String scriptName = "forward.yaml";
        final String url = "https://www.forwardhealth.wi.gov/WIPortal/Subsystem/Provider/DrugSearch.aspx";

        // Start the crawl
        runScript(url, scriptName);
    }


    @Test
    public void testHumanServePA() throws Exception {
        // Crawl parameters
        final String scriptName = "humanservepa.yaml";
        final String url = "https://www.humanservices.state.pa.us/COVEREDDRUGS";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void testLogicBlocks() throws Exception {
        // Crawl parameters
        final String scriptName = "logic-blocks.yaml";
        final String url = "https://www.humanservices.state.pa.us/COVEREDDRUGS";

        // Start the crawl
        final Map<String, Object> script = loadScript(scriptName);
        Set<String> keys = script.keySet();
        for(String key : keys) {
            List<Map<String, String>> thing = (List<Map<String, String>>) script.get(key);
            for (Map<String, String> pair : thing) {
                System.out.println(pair);
            }
        }
    }
}
