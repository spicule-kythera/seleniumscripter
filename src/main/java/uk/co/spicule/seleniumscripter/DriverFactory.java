package uk.co.spicule.seleniumscripter;


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
    boolean headless; // Toggles headless mode
    final List<String> options; // Browser options

    public DriverFactory(List<String> options) {
        this.options = options;
    }

    public boolean setHeadless(boolean headless) {
        this.headless = headless;
        return this.headless;
    }

    public RemoteWebDriver generateChromeDriver() {
        // Create and populate driver options
        ChromeOptions driverOptions = new ChromeOptions();
        options.forEach(driverOptions::addArguments);
        driverOptions.setHeadless(headless);

        // Set a load strategy
        //driverOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        // Create and load the driver with options
        ChromeDriver driver = new ChromeDriver(driverOptions);
//        driver.manage().timeouts().pageLoadTimeout(3600, TimeUnit.SECONDS);

        return driver;
    }

    public RemoteWebDriver generateFirefoxDriver() {
        // Create and populate driver options
        FirefoxOptions driverOptions = new FirefoxOptions();
        options.forEach(driverOptions::addArguments);
        driverOptions.setHeadless(headless);

        // Create and load the driver with options
        return new FirefoxDriver(driverOptions);
    }

    public RemoteWebDriver generateEdgeDriver() {
        // Create and populate driver options
        EdgeOptions driverOptions = new EdgeOptions();
        // options.forEach(driverOptions::addArguments); // Apparently Edge doesn't support options?
        // driverOptions.setHeadless(headless); // Doesn't support headless either apparently

        // Create and load the driver with options
        return new EdgeDriver(driverOptions);
    }
}