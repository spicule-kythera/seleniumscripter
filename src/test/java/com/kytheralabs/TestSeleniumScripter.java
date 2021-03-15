package com.kytheralabs;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
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

        SeleniumScripter s = new SeleniumScripter();
        s.runScript(obj);
    }
}
