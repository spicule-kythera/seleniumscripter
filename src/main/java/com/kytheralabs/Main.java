package com.kytheralabs;

import java.util.Map;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;

public class Main {
    private static Map<String, Object> loadScript(String fileName) {
        InputStream inputStream = Main.class
                                      .getClassLoader()
                                      .getResourceAsStream(fileName);
        return new Yaml().load(inputStream);
    }

    public static void main(String[] argv) throws Exception {
        Map<String, Object> script = loadScript("logic-blocks.yaml");
        MagnesiumScriptCompiler compiler = new MagnesiumScriptCompiler();

        try {
            compiler.Compile(script);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}