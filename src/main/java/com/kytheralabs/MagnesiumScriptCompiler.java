package com.kytheralabs;

import java.util.Map;

public class MagnesiumScriptCompiler {
    private static final Integer MAX_RECURSION_DEPTH = 2;



    public void Compile(Map<String, Object> script) throws Exception{
        compile(script, 0);
    }

    public void compile(Map<String, Object> script, Integer depth) throws Exception {
        // Set a recursion limit
        if(depth >= MAX_RECURSION_DEPTH) {
            return;
        }

        script.forEach((String name, Object subscript) -> {
            // Resolve meta-operations
            if(name.equals("if")){
                Map<String, Object> content = (Map<String, Object>) subscript;
                String condition = content.get("condition").toString();
                Map<String, Object> thenBlock = (Map<String, Object>) content.get("then");
                Map<String, Object> elseBlock = (Map<String, Object>) content.get("else");

                if(condition.equals("true")) {
                    compile(thenBlock, depth + 1);
                }
                else {
                    compile(elseBlock, depth + 1);
                }

                System.out.println(condition);
            }
            else {

            }
        });
    }
}
