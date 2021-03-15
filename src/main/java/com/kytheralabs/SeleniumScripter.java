package com.kytheralabs;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeleniumScripter {
    private WebDriver driver;
    private Map<String, Object> masterScript;
    private Map<String, List<String>> captureLists = new HashMap<>();
    private List<String> snapshots = new ArrayList<>();
    private Integer iteration = 0;
    private String loopValue;

    public SeleniumScripter(WebDriver webDriver){
        driver = webDriver;
    }
    public void runScript(Map<String, Object> script, Integer iteration, String loopValue) throws Exception {
        System.out.println("Processing Selenium Script");
        System.out.println("Objects found: "+script.size());
        this.iteration = iteration;
        this.loopValue = loopValue;
        if(masterScript == null){
            masterScript = script;
        }
        for (Map.Entry me : script.entrySet()) {
            System.out.println("Key: "+me.getKey() + " & Value: " + me.getValue());
            if(me.getValue() instanceof Map){
                Map<String, Object> obj = (Map<String, Object>) me.getValue();
                System.out.println("Operation found: "+ obj.get("operation"));
                if(obj.containsKey("operation")) {
                    if (obj.get("operation").equals("select")) {
                        runSelect(obj);
                    } else if (obj.get("operation").equals("keys")) {
                        runKeys(obj);
                    } else if (obj.get("operation").equals("wait")) {
                        runWait(obj);
                    } else if (obj.get("operation").equals("captureList")) {
                        captureList(obj);
                    } else if (obj.get("operation").equals("loop")) {
                        loop(obj);
                    } else if (obj.get("operation").equals("click")) {
                        click(obj);
                    } else if (obj.get("operation").equals("clickListItem")) {
                        clickListItem(obj);
                    } else if (obj.get("operation").equals("snapshot")) {
                        snapshot();
                    }
                }
            }

        }
    }

    private WebElement selectElement(String selector, String name) throws Exception {
        WebElement clickedEl = null;
        switch (selector) {
            case "id":
                clickedEl = driver.findElement(By.id(name));
                break;
            case "class":
                clickedEl = driver.findElement(By.className(name));
                break;
            case "cssSelector":
                clickedEl = driver.findElement(By.cssSelector(name));
                break;
            case "xpath":
                clickedEl = driver.findElement(By.xpath(name));
                break;
            case "name":
                clickedEl = driver.findElement(By.name(name));
                break;
        }

        if(clickedEl != null){
            return clickedEl;
        } else{
            throw new Exception("Could not find element");
        }
    }

    private List<WebElement> selectElements(String selector, String name) throws Exception {
        List<WebElement> clickedEl = null;
        switch (selector) {
            case "id":
                clickedEl = driver.findElements(By.id(name));
                break;
            case "class":
                clickedEl = driver.findElements(By.className(name));
                break;
            case "cssSelector":
                clickedEl = driver.findElements(By.cssSelector(name));
                break;
            case "xpath":
                clickedEl = driver.findElements(By.xpath(name));
                break;
            case "name":
                clickedEl = driver.findElements(By.name(name));
                break;
        }

        if(clickedEl != null){
            return clickedEl;
        } else{
            throw new Exception("Could not find element");
        }
    }

    private By ByElement(String selector, String name) {
        switch (selector) {
            case "id":
                return By.id(name);
            case "class":
                return By.className(name);
            case "cssSelector":
                return By.cssSelector(name);
            case "xpath":
                return By.xpath(name);
            case "name":
                return By.name(name);
        }

        return null;
    }

    private void runSelect(Map<String, Object> script) throws Exception {
        WebElement element = selectElement(script.get("selector").toString(), script.get("name").toString());
        Select selectObj = new Select(element);
        if(script.get("selectBy").equals("value")){
            System.out.println("Run select by value");
            selectObj.selectByValue(script.get("value").toString());
        } else if(script.get("selectBy").equals("index")){
            System.out.println("Run select by index");
            selectObj.selectByIndex(Integer.parseInt(script.get("value").toString()));
        } else if(script.get("selectBy").equals("visible")){
            System.out.println("Run select by visible text");
            selectObj.selectByVisibleText(script.get("value").toString());
        }
    }

    private void runKeys(Map<String, Object> script) throws Exception {
        WebElement element = selectElement(script.get("selector").toString(), script.get("name").toString());
        String keystring = script.get("value").toString();
        String target = keystring;
        if(keystring.equals("${loopvalue}")){
            target = this.loopValue;
        }
        element.clear();
        for(char s : target.toCharArray()){
            System.out.println("Inserting: "+String.valueOf(s));
            element.sendKeys(String.valueOf(s));
            Thread.sleep(300);
        }
    }

    private void runWait(Map<String, Object> script) throws Exception {
        int waittimeout = 30;
        if(script.containsKey("timeout")){
            waittimeout = Integer.parseInt(script.get("timeout").toString());
        }
        WebDriverWait wait = new WebDriverWait(driver, waittimeout);
        System.out.println("Waiting for object: "+script.get("name").toString());
        wait.until(ExpectedConditions.visibilityOfElementLocated(ByElement(script.get("selector").toString(), script.get("name").toString())));
        System.out.println("Object found");
    }

    private void captureList(Map<String, Object> script) throws Exception {
        System.out.println("Generating Capture List");
        List<WebElement> webElements = selectElements(script.get("selector").toString(), script.get("name").toString());
        List<String> strlist = new ArrayList<>();
        for(WebElement el : webElements){
            System.out.println("Capture Element Found: "+el.getText());
            strlist.add(el.getText());
        }
        System.out.println("Storing capture list as: "+script.get("variable").toString());
        captureLists.put(script.get("variable").toString(), strlist);
    }

    private void loop(Map<String, Object> script) throws Exception {
        String loopType = script.get("type").toString();
        if(loopType.equals("variable")){
            List<String> vars = captureLists.get(script.get("variable").toString());
            System.out.println("Performing Variable Loop for: "+script.get("variable").toString());
            for (String v : vars) {
                Map<String, Object> subscripts = (Map<String, Object>) masterScript.get("subscripts");
                Map<String, Object> subscript = (Map<String, Object>) subscripts.get(script.get("subscript"));
                System.out.println("Looping for variable: " + v+ " . Using subscript: "+ script.get("subscript"));
                runScript(subscript, null, v);
            }
        }
    }

    private void click(Map<String, Object> script) throws Exception {
        WebElement element = selectElement(script.get("selector").toString(), script.get("name").toString());
        System.out.println("Clicking Element");
        element.click();
    }

    private void clickListItem(Map<String, Object> script) throws Exception {
        List<WebElement> element = selectElements(script.get("selector").toString(), script.get("name").toString());
        int i = Integer.parseInt(script.get("item").toString());
        System.out.println("Clicking list item");
        element.get(i).click();
    }

    private void snapshot(){
        System.out.println("Taking Snapshot");
        snapshots.add(driver.getPageSource());
    }




}
