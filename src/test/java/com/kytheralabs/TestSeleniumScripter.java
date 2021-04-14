package com.kytheralabs;

import org.junit.Test;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class TestSeleniumScripter {

    //To test at Jar level
    @Test
    public void testRunScript() throws Exception {
        Yaml yaml = new Yaml();
        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("example3.yaml");
        Map<String, Object> obj = yaml.load(inputStream);
        System.out.println(obj);

        final ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--ignore-certificate-errors");
        System.setProperty("webdriver.chrome.driver", "classes/chromedriver.exe");
        ChromeDriver webDriver = new ChromeDriver();
        webDriver.get("https://communitysolutions.pacificsource.com/Search/Drug/Name");
        SeleniumScripter scripter = new SeleniumScripter(webDriver);
        scripter.runScript(obj, 1, 0);

    }

    //To test at IDE level
    @Test
    public void testRunAlabama() throws Exception {
            Yaml yaml = new Yaml();
            InputStream inputStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("example2.yaml");
            Map<String, Object> obj = yaml.load(inputStream);
            System.out.println(obj);

            final ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("--ignore-certificate-errors");
            System.setProperty("webdriver.chrome.driver", "/home/bugg/chromedriver");
            ChromeDriver webDriver = new ChromeDriver();
            webDriver.get("https://rxtools.aetnamedicare.com/helpfultools/2021/Resources/HelpfulTools");
            SeleniumScripter scripter = new SeleniumScripter(webDriver);
            scripter.runScript(obj, 1, 0);
    }
        //Future proxy intergration?
//    @Test
//    public void testRunScript() throws Exception {
//        Yaml yaml = new Yaml();
//        InputStream inputStream = this.getClass()
//                .getClassLoader()
//                .getResourceAsStream("example.yaml");
//        Map<String, Object> obj = yaml.load(inputStream);
//        System.out.println(obj);
//
//        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
//        final ChromeOptions chromeOptions = new ChromeOptions();
//        chromeOptions.addArguments("--no-sandbox");
//        chromeOptions.addArguments("--headless");
//        chromeOptions.addArguments("--ignore-certificate-errors");
//        //capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
//        String loc = "http://localhost:3000/webdriver";
//        capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
//
//
//        WebDriver webDriver = new RemoteWebDriver(new URL(loc), capabilities);
//
//        webDriver.get("https://www.upmchealthplan.com/find-a-medication/default.aspx#medication");
//        SeleniumScripter s = new SeleniumScripter(webDriver);
//        s.runScript(obj, null, null);
//    }
}
