package com.kytheralabs;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.yaml.snakeyaml.Yaml;

import javax.management.AttributeNotFoundException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TestSeleniumScripter {
    private final String browserType = BrowserType.CHROME; // Type of driver to use
    private final List<String> options = Arrays.asList("--no-sandbox",
                                                       "--headless",
                                                       "--disable-gpu",
                                                       "--disable-extensions",
                                                       "--ignore-certificate-errors",
                                                       "--incognito",
                                                       "--window-size=1920,1080",
                                                       "--proxy-server='direct://",
                                                       "--proxy-bypass-list=*",
                                                       "--disable-background-networking",
                                                       "--safebrowsing-disable-auto-update",
                                                       "--disable-sync",
                                                       "--metrics-recording-only",
                                                       "--disable-default-apps",
                                                       "--no-first-run",
                                                       "--disable-setuid-sandbox",
                                                       "--hide-scrollbars",
                                                       "--no-zygote",
                                                       "--disable-notifications",
                                                       "--disable-logging",
                                                       "--disable-permissions-api");
    private RemoteWebDriver driver = null;

    @Before
    public void setUp() {
        // Create driver factory
        DriverFactory factory = new DriverFactory(options);

        // Create driver
        switch (browserType) {
            case BrowserType.CHROME:
                driver = factory.generateChromeDriver();
                break;
            case BrowserType.EDGE:
                driver = factory.generateEdgeDriver();
                break;
            case BrowserType.FIREFOX:
                driver = factory.generateFirefoxDriver();
                break;
            default:
                throw new ValueException("Invalid browser type: " + browserType);
        }
    }

    @After
    public void tearDown() {
        driver.close();
        driver = null;
    }

    private static Map<String, Object> loadJSONScript(String filename) throws IOException, ParseException {
        URL filepath = TestSeleniumScripter.class.getClassLoader().getResource(filename);
        if(filepath == null) {
            throw new FileNotFoundException("Embedded resource not found: " + filename);
        }
        FileReader reader = new FileReader(filepath.getPath());
        Map<String, Object> map = (Map<String, Object>) new JSONParser().parse(reader);
        return new TreeMap<>(map);
    }

    private static Map<String, Object> loadYAMLScript(String filename) {
        InputStream inputStream = TestSeleniumScripter.class
                                                      .getClassLoader()
                                                      .getResourceAsStream(filename);
        return new Yaml().load(inputStream);
    }

    private void runScript(String url, String scriptName) throws Exception {
        String[] parts = scriptName.split("\\.");
        String extension = parts[parts.length - 1];
        System.out.println("Using " + extension + " parser!");
        runScript(url, scriptName, extension);
    }

    private void runScript(String url, String scriptName, String scriptType) throws IOException,
                                                                                    AttributeNotFoundException,
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
        }

        driver.get(url);
        SeleniumScripter scriptRunner = new SeleniumScripter(driver, true);
        scriptRunner.runScript(script);
        System.out.println("Took " + scriptRunner.getSnapshots().size() + " snapshots for this agent!");
    }

    @Ignore
    @Test
    public void demo() throws Exception {
        // Crawl parameters
        final String scriptName = "demo.yaml";
        final String url = "https://www.forwardhealth.wi.gov/WIPortal/Subsystem/Provider/DrugSearch.aspx";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Ignore
    @Test
    public void alabama() throws Exception {
        // Crawl parameters
        final String scriptName = "alabama.yaml";
        final String url = "https://www.medicaid.alabamaservices.org/ALPortal/NDC%20Look%20Up/tabId/39/Default.aspx";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void forward() throws Exception {
        // Crawl parameters
        final String scriptName = "forward.yaml";
        final String url = "https://www.forwardhealth.wi.gov/WIPortal/Subsystem/Provider/DrugSearch.aspx";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void humanServePA() throws Exception {
        // Crawl parameters
        final String scriptName = "humanservepa.yaml";
        final String url = "https://www.humanservices.state.pa.us/COVEREDDRUGS";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void optum() throws Exception {
        // Crawl parameters
        final String scriptName = "optumfixed3.yaml";
        final String url = "https://www.optumrx.com/clientformulary/formulary.asp?var=UCSPAQ6&infoid=UCSPAQ6&page=insert&par=";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void upmc() throws Exception {
        // Crawl parameters
        final String scriptName = "upmc2.json";
        final String url = "https://www.upmchealthplan.com/find-a-medication/default.aspx#medication";

        // Start the crawl
        try {
            runScript(url, scriptName);
        } catch(Exception e){
            System.out.println("here");
        }
    }

    @Test
    public void cmsgov() throws Exception {
        // Crawl parameters
        final String scriptName = "cmsgov.yaml";
        final String url = "https://www.cms.gov/medicare-coverage-database/new-search/handlers/tour-end.ashx?t=1625154700997&which=report";

        // Start the crawl

        runScript(url, scriptName);
    }

    @Test
    public void myprime() throws Exception{
        final String scriptName = "myprime.yaml";
        final String url = "https://www.myprime.com/es/boeing/plan-preview/medicines.html#find-medicine";


            runScript(url, scriptName);


    }
}
