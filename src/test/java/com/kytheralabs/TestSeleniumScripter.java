package com.kytheralabs;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestSeleniumScripter {
    private FirefoxOptions driverOptions = null;
    private final List<String> options = Arrays.asList("--no-sandbox",
                                                       "--log-level=3",
                                                       "--ignore-certificate-errors",
                                                       "--headless",
                                                       "--window-size=1920,1080",
                                                       "--start-maximized",
                                                       "--disable-gpu",
                                                       "--disable-extensions",
                                                       "--disable-infobars");
    private RemoteWebDriver driver = null;

    @Before
    public void setUp() {
        driverOptions = new FirefoxOptions();
        options.forEach(driverOptions::addArguments);
        driver = new FirefoxDriver(driverOptions);
    }

    @After
    public void tearDown() {
        driver.close();
        driver = null;
        driverOptions = null;
    }

    private static Map<String, Object> loadJSONScript(String filename) throws IOException, ParseException {
        URL filepath = TestSeleniumScripter.class.getClassLoader().getResource(filename);
        if(filepath == null) {
            throw new FileNotFoundException("Embedded resource not found: " + filename);
        }
        FileReader reader = new FileReader(filepath.getPath());
        return (Map<String, Object>) new JSONParser().parse(reader);
    }

    private static Map<String, Object> loadYAMLScript(String filename) {
        InputStream inputStream = TestSeleniumScripter.class
                                                      .getClassLoader()
                                                      .getResourceAsStream(filename);
        return new Yaml().load(inputStream);
    }

    private void runScript(String url, String scriptName) throws Exception {
        String extension = scriptName.split("\\.")[1];
        runScript(url, scriptName, extension);
    }

    private void runScript(String url, String scriptName, String scriptType) throws IOException,
                                                                                    ParseException,
                                                                                    java.text.ParseException,
                                                                                    InterruptedException {
        final Map<String, Object> script = (Map<String, Object>) switch (scriptType.toLowerCase()) {
            case "json" -> loadJSONScript(scriptName);
            case "yaml" -> loadYAMLScript(scriptName);
            default -> throw new IllegalArgumentException("Unsuported script type: " + scriptType);
        };
        driver.get(url);
        SeleniumScripter scriptRunner = new SeleniumScripter(driver);
        scriptRunner.runScript(script);
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

    @Ignore
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

    @Ignore
    @Test
    public void testForwardJSON() throws Exception {
        // Crawl parameters
        final String scriptName = "forward.json";
        final String url = "https://www.forwardhealth.wi.gov/WIPortal/Subsystem/Provider/DrugSearch.aspx";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Ignore
    @Test
    public void testHumanServePA() throws Exception {
        // Crawl parameters
        final String scriptName = "humanservepa.yaml";
        final String url = "https://www.humanservices.state.pa.us/COVEREDDRUGS";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Ignore
    @Test
    public void testLogicBlocks() throws Exception {
        // Crawl parameters
        final String scriptName = "logic-blocks.yaml";
        final String url = "https://www.humanservices.state.pa.us/COVEREDDRUGS";

        // Start the crawl
        runScript(url, scriptName);
    }
}
