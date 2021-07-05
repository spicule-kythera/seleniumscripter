package com.kytheralabs;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BrowserConfig {
    public static RemoteWebDriver newChromeWebDriver() {

        final ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        //chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-extensions");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--incognito");
        chromeOptions.addArguments("--window-size=1920,1080");
        chromeOptions.addArguments("--proxy-server='direct://");
        chromeOptions.addArguments("--proxy-bypass-list=*");
        chromeOptions.addArguments("--disable-background-networking");
        chromeOptions.addArguments("--safebrowsing-disable-auto-update");
        chromeOptions.addArguments("--disable-sync");
        chromeOptions.addArguments("--metrics-recording-only");
        chromeOptions.addArguments("--disable-default-apps");
        chromeOptions.addArguments("--no-first-run");
        chromeOptions.addArguments("--disable-setuid-sandbox");
        chromeOptions.addArguments("--hide-scrollbars");
        chromeOptions.addArguments("--no-zygote");
        chromeOptions.addArguments("--disable-notifications");
        chromeOptions.addArguments("--disable-logging");
        chromeOptions.addArguments("--disable-permissions-api");

        chromeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        //capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);


        ChromeDriver wdriver = new ChromeDriver(chromeOptions);
        wdriver.manage().timeouts().pageLoadTimeout(3600, TimeUnit.SECONDS);


        return wdriver;
    }

    public static RemoteWebDriver newFirefoxWebDriver() {


        List<String> options = Arrays.asList("--no-sandbox",
                "--log-level=3",
                "--ignore-certificate-errors",
                "--start-maximized",
                "--disable-gpu",
                "--headless",
                "--disable-extensions",
                "--disable-infobars");

        FirefoxOptions driverOptions = new FirefoxOptions();
        options.forEach(driverOptions::addArguments);
        FirefoxDriver wdriver = new FirefoxDriver(driverOptions);



        return wdriver;
    }
}
