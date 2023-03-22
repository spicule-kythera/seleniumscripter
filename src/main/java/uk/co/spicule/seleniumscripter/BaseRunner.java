package uk.co.spicule.seleniumscripter;

import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.RemoteWebDriver;

import javax.management.AttributeNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class BaseRunner {
    private boolean headless = false;
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
    private RemoteWebDriver driver = null;


    public void runScript(String content, String url) throws IOException,
            AttributeNotFoundException,
            java.text.ParseException,
            InterruptedException,
            SeleniumScripter.StopIteration{
        final Map<String, Object> script;
        script = new Yaml().load(content);

        // Set logging level to debug
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "Debug");

        // Create driver factory
        DriverFactory factory = new DriverFactory(options);
        factory.setHeadless(headless);

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
                throw new ParseException("Invalid browser type: " + browserType, 0);
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
}
