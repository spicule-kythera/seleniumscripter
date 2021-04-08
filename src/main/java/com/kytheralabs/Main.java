package com.kytheralabs;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {

        //Chrome Browser support

        Yaml yaml = new Yaml();
        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(args[0]);
        Map<String, Object> obj = yaml.load(inputStream);
        System.out.println(obj);

        final ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--ignore-certificate-errors");
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        ChromeDriver webDriver = new ChromeDriver();
        webDriver.get(args[1]);
        SeleniumScripter scripter = new SeleniumScripter(webDriver);
        scripter.runScript(obj, 1, 0);


        //Firefox browser support

//        Yaml yaml = new Yaml();
//        InputStream inputStream = Thread.currentThread()
//                .getContextClassLoader()
//                .getResourceAsStream(args[0]);
//        Map<String, Object> obj = yaml.load(inputStream);
//        System.out.println(obj);
//
//        final FirefoxOptions firefoxOptions = new FirefoxOptions();
//        firefoxOptions.addArguments("--no-sandbox");
//        firefoxOptions.addArguments("--headless");
//        firefoxOptions.addArguments("--ignore-certificate-errors");
//        System.setProperty("webdriver.gecko.driver", "geckodriver.exe");
//        FirefoxDriver webDriver = new FirefoxDriver();
//        webDriver.get(args[1]);
//        SeleniumScripter scripter = new SeleniumScripter(webDriver);
//        scripter.runScript(obj, 1, 0);

    }
}
