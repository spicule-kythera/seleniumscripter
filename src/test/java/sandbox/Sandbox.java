package sandbox;

import java.net.URL;
import java.util.Map;
import java.util.List;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PersistentCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import uk.co.spicule.seleniumscripter.SeleniumScripter;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.util.Arrays;
import java.util.TreeMap;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;
import java.io.FileNotFoundException;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.RemoteWebDriver;
import javax.management.AttributeNotFoundException;

public class Sandbox {
    private final String browserType = BrowserType.CHROME; // Type of driver to use
    private final List<String> options = Arrays.asList("--no-sandbox",
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
    private WebDriver driver = null;

    @Before
    public void setUp()  {
        // Set logging level to debug
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "Debug");

        // Create driver factory
        FirefoxOptions options = new FirefoxOptions();
        this.options.forEach(options::addArguments);
        Capabilities capabilities = new PersistentCapabilities(options);

        driver = new FirefoxDriver();
        driver.manage().window().maximize();
    }

    @After
    public void tearDown() {
        //driver.close(); // Deprecated?
        driver = null;
    }

    private static Map<String, Object> loadJSONScript(String filename) throws IOException,
                                                                              org.json.simple.parser.ParseException {
        URL filepath = Sandbox.class.getClassLoader().getResource(filename);
        if(filepath == null) {
            throw new FileNotFoundException("Embedded resource not found: " + filename);
        }
        FileReader reader = new FileReader(filepath.getPath());
        Map<String, Object> map = (Map<String, Object>) new JSONParser().parse(reader);
        return new TreeMap<>(map);
    }

    private static Map<String, Object> loadYAMLScript(String filename) {
        InputStream inputStream = Sandbox.class
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
                                                                                    java.text.ParseException,
                                                                                    InterruptedException,
                                                                                    SeleniumScripter.StopIteration,
                                                                                    org.json.simple.parser.ParseException {
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

        // Set the default path
        String path = System.getProperty("user.home");
        path += (path.endsWith("/") ? "" : "/") + "Documents/work/dump/";
        scriptRunner.setOutputPath(path);

        scriptRunner.runScript(script);
        System.out.println("Took " + scriptRunner.getSnapshots().size() + " snapshots for this agent!");
    }

    @Test
    public void aetna() throws Exception {
        // Crawl parameters
        final String scriptName = "aetna.yaml";
        final String url = "https://rxtools.aetnamedicare.com/helpfultools/2021/Resources/HelpfulTools";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void alabama() throws Exception {
        // Crawl parameters
        final String scriptName = "alabama.yaml";
        final String url = "https://www.medicaid.alabamaservices.org/ALPortal/NDC%20Look%20Up/tabId/39/Default.aspx";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void cmsGov() throws Exception {
        // Crawl parameters
        final String scriptName = "cmsgov.yaml";
        final String url = "https://www.cms.gov/medicare-coverage-database/new-search/handlers/tour-end.ashx?t=1625154700997&which=report";

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
    public void myPrime() throws Exception {
        // Crawl parameters
        final String scriptName = "myprime.yaml";
        final String url = "https://www.myprime.com/es/boeing/plan-preview/medicines.html#find-medicine";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void optum() throws Exception {
        // Crawl parameters
        final String scriptName = "optum-groovy.yaml";
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
        runScript(url, scriptName);
    }

    @Test
    public void screenshot() throws Exception {
        // Crawl parameters
        final String scriptName = "screenshot.yaml";
        final String url = "https://news.bbc.co.uk";

        // Start the crawl
        runScript(url, scriptName);
    }

    @Test
    public void bcbsms() throws Exception {
        // Crawl parameters
        final String scriptName = "bcbsms.yaml";
        final String url = "https://www.bcbsms.com/BlueLand/rx/rxDirectFormularyDrugSearch.do";
        // Start the crawl
        runScript(url, scriptName);
    }
}
