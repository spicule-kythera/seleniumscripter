package com.kytheralabs;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {


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
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        WebDriver webDriver = new ChromeDriver();
        webDriver.get("https://www.medicaid.alabamaservices.org/ALPortal/NDC%20Look%20Up/tabId/39/Default.aspx");
        SeleniumScripter scripter = new SeleniumScripter(webDriver);
        scripter.runScript(obj, 1, 0);

    }
}
