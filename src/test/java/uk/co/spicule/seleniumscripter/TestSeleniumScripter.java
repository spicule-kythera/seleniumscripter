package uk.co.spicule.seleniumscripter;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

public class TestSeleniumScripter {
    @Test
    protected void sliceIsValid() throws ParseException {
        String slice = "1:-1";
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> expected = Arrays.asList(1, 2, 3, 4);

        List<Integer> slicedList = SeleniumScripter.slice(slice, list);
        Assertions.assertEquals(slicedList, expected);
    }
}
