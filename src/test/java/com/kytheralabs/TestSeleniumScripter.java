package com.kytheralabs;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public class TestSeleniumScripter {

    @Test
    public void testRunScript() throws Exception {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("example.yaml");
        Map<String, Object> obj = yaml.load(inputStream);
        System.out.println(obj);

        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        final ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--ignore-certificate-errors");
        //capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
        String loc = "http://localhost:3000/webdriver";
        capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);


        WebDriver webDriver = new RemoteWebDriver(new URL(loc), capabilities);

        webDriver.get("https://www.upmchealthplan.com/find-a-medication/default.aspx#medication");
        SeleniumScripter s = new SeleniumScripter(webDriver);
        s.runScript(obj, null, null);
    }

    @Test
    public void testRunAlabama() throws Exception {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("example2.yaml");
        Map<String, Object> obj = yaml.load(inputStream);
        System.out.println(obj);

        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        final ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--ignore-certificate-errors");
        //capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
        String loc = "http://localhost:3000/webdriver";
        capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);


        WebDriver webDriver = new RemoteWebDriver(new URL(loc), capabilities);

        webDriver.get("https://www.medicaid.alabamaservices.org/ALPortal/NDC%20Look%20Up/tabId/39/Default.aspx");
        SeleniumScripter s = new SeleniumScripter(webDriver);
        s.runScript(obj, null, null);
    }
}
