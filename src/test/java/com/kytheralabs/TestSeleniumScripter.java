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
import java.util.*;

public class TestSeleniumScripter {
    private FirefoxOptions driverOptions = null;
    private final List<String> options = Arrays.asList("--no-sandbox",
                                                       "--log-level=3",
                                                       "--headless",
                                                       "--ignore-certificate-errors",
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
        Map<String, Object> map = (Map<String, Object>) new JSONParser().parse(reader);
        Map<String, Object> treeMap = new TreeMap<>(map);
        return treeMap;
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
        System.out.println("URL: " + url);
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

    @Ignore
    @Test
    public void example() throws Exception {
        // Crawl parameters
        final String scriptName = "example.yaml";
        final String url = "https://www.upmchealthplan.com/find-a-medication/default.aspx#medication";

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
    public void humanServePA() throws Exception {
        // Crawl parameters
        final String scriptName = "humanservepa.yaml";
        final String url = "https://www.humanservices.state.pa.us/COVEREDDRUGS";

        // Start the crawl
        assert runScript(url, scriptName);
    }

    @Ignore
    @Test
    public void jsBack() throws Exception {
        // Crawl parameters
        final String scriptName = "js-back.yaml";
        final String url = "https://www.nasa.gov";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void optum() throws Exception {
        // Crawl parameters
        final String scriptName = "optum.json";
        final String url = "https://www.optumrx.com/ClientFormulary/formulary.asp?var=PHSCA&infoid=PHSCA";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void upmc() throws Exception {
        // Crawl parameters
        final String scriptName = "upmc.json";
        final String url = "https://www.upmchealthplan.com/find-a-medication/default.aspx#medication";

        // Start the crawl
        assert runScript(url, scriptName);
    }
}
