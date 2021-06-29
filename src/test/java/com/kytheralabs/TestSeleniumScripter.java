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
                                                       //"--headless",
                                                       "--ignore-certificate-errors",
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

    private boolean runScript(String url, String scriptName) throws Exception {
        String[] parts = scriptName.split("\\.");
        String extension = parts[parts.length - 1];
        System.out.println("Using " + extension + " parser!");
        return runScript(url, scriptName, extension);
    }

    private boolean runScript(String url, String scriptName, String scriptType) throws IOException,
                                                                                    ParseException,
                                                                                    java.text.ParseException,
                                                                                    InterruptedException {
        final Map<String, Object> script;
        switch (scriptType.toLowerCase()) {
            case "json":
                script = loadJSONScript(scriptName);
                break;
            case "yml":
            case "yaml":
                script = loadYAMLScript(scriptName);
                break;
            default:
                throw new IllegalArgumentException("Unsupported script type: " + scriptType);
        };
        driver.get(url);
        SeleniumScripter scriptRunner = new SeleniumScripter(driver);
        return scriptRunner.runScript(script);
    }

    @Ignore
    @Test
    public void alabama() throws Exception {
        // Crawl parameters
        final String scriptName = "alabama.yaml";
        final String url = "https://www.medicaid.alabamaservices.org/ALPortal/NDC%20Look%20Up/tabId/39/Default.aspx";

        // Start the crawl
        assert runScript(url, scriptName);
    }

    @Test
    public void bcbsms() throws Exception {
        // Crawl parameters
        final String scriptName = "bcbsms.yaml";
        final String url = "https://www.bcbsms.com/BlueLand/rx/rxDirectFormularyDrugSearch.do?year=2017&dotcom=true";

        // Start the crawl
        assert runScript(url, scriptName);
    }

    @Test
    public void forward() throws Exception {
        // Crawl parameters
        final String scriptName = "forward.yaml";
        final String url = "https://www.forwardhealth.wi.gov/WIPortal/Subsystem/Provider/DrugSearch.aspx";

        // Start the crawl
        assert runScript(url, scriptName);
    }

    @Test
    public void forwardJSON() throws Exception {
        // Crawl parameters
        final String scriptName = "forward.json";
        final String url = "https://www.forwardhealth.wi.gov/WIPortal/Subsystem/Provider/DrugSearch.aspx";

        // Start the crawl
        assert runScript(url, scriptName);
    }

    @Test
    public void humanServePA() throws Exception {
        // Crawl parameters
        final String scriptName = "humanservepa.yaml";
        final String url = "https://www.humanservices.state.pa.us/COVEREDDRUGS";

        // Start the crawl
        assert runScript(url, scriptName);
    }

    @Test
    public void logicBlocks() throws Exception {
        // Crawl parameters
        final String scriptName = "logic-blocks.yaml";
        final String url = "https://www.humanservices.state.pa.us/COVEREDDRUGS";

        // Start the crawl
        assert runScript(url, scriptName);
    }

    @Test
    public void upmc() throws Exception {
        // Crawl parameters
        final String scriptName = "upmc.json";
        final String url = "https://www.upmchealthplan.com/find-a-medication/default.aspx#medication";

        // Start the crawl
        assert runScript(url, scriptName);
    }

    @Test
    public void testBack() throws Exception {
        // Crawl parameters
        final String scriptName = "testback.yaml";
        final String url = "https://www.nasa.gov";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void testOptum() throws Exception {
        // Crawl parameters
        final String scriptName = "optum.yaml";
        final String url = "https://www.optumrx.com/oe_rxexternal/prescription-drug-list?type=ClientFormulary&var=PHSCA&infoid=PHSCA";

        // Start the crawl
        runScript(url, scriptName);
    }
}
