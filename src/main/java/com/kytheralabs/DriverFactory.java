package com.kytheralabs;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DriverFactory {
    final List<String> options; // Browser options

    DriverFactory(List<String> options) {
        this.options = options;
    }

    public RemoteWebDriver generateChromeDriver() {
        // Create and populate driver options
        ChromeOptions chromeOptions = new ChromeOptions();
        options.forEach(chromeOptions::addArguments);

        // Set a load strategy
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        //capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

        // Create and load the driver with options
        ChromeDriver driver = new ChromeDriver(chromeOptions);
        driver.manage().timeouts().pageLoadTimeout(3600, TimeUnit.SECONDS);

        return driver;
    }

    public RemoteWebDriver generateFirefoxDriver() {
        // Create and populate driver options
        FirefoxOptions driverOptions = new FirefoxOptions();
        options.forEach(driverOptions::addArguments);

        // Create and load the driver with options
        return new FirefoxDriver(driverOptions);
    }

    public RemoteWebDriver generateEdgeDriver() {
        // Create and populate driver options
        EdgeOptions driverOptions = new EdgeOptions();
        // options.forEach(driverOptions::addArguments); // Apparently Edge doesn't support options?

        // Create and load the driver with options
        return new EdgeDriver(driverOptions);
    }
}
