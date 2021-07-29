package com.kytheralabs;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.platform.commons.annotation.Testable;
import org.openqa.selenium.WebDriver;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

public class Spec_SeleniumScripter extends TestCase {

    @Before
    protected void setUp() {
        DriverFactory factory = new DriverFactory(null);
        WebDriver driver = factory.generateFirefoxDriver();
        @Testable SeleniumScripter scripter = new SeleniumScripter(driver);
    }

    @Test
    protected void sliceIsValid() throws ParseException {
        String slice = "1:-1";
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> expected = Arrays.asList(1, 2, 3, 4);

        List<Integer> slicedList = SeleniumScripter.slice(slice, list);
        assertSame(slicedList, expected);
    }
}
