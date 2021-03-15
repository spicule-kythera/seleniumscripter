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
    private Map<String, List<ArrayList>> captureLists = new HashMap<>();
    private List<String> snapshots = new ArrayList<>();

    public void runScript(Map<String, Object> script) throws Exception {
        System.out.println("Processing Selenium Script");
        System.out.println("Objects found: "+script.size());
        masterScript = script;
        for (Map.Entry me : script.entrySet()) {
            System.out.println("Key: "+me.getKey() + " & Value: " + me.getValue());
            if(me.getValue() instanceof Map){
                Map<String, Object> obj = (Map<String, Object>) me.getValue();
                if(obj.get("operation").equals("select")){
                    runSelect(obj);
                } else if(obj.get("operation").equals("keys")){
                    runKeys(obj);
                } else if(obj.get("operation").equals("wait")){
                    runWait(obj);
                } else if(obj.get("operation").equals("captureList")){
                    captureList(obj);
                } else if(obj.get("operation").equals("loop")){
                    loop(obj);
                } else if(obj.get("operation").equals("click")){
                    click(obj);
                } else if(obj.get("operation").equals("clickListItem")){
                    clickListItem(obj);
                } else if(obj.get("operation").equals("snapshot")){
                    snapshot();
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
            selectObj.selectByValue(script.get("value").toString());
        } else if(script.get("selectBy").equals("index")){
            selectObj.selectByIndex(Integer.parseInt(script.get("value").toString()));
        } else if(script.get("selectBy").equals("visible")){
            selectObj.selectByVisibleText(script.get("value").toString());
        }
    }

    private void runKeys(Map<String, Object> script) throws Exception {
        WebElement element = selectElement(script.get("selector").toString(), script.get("name").toString());
        String keystring = script.get("value").toString();
        element.sendKeys(keystring);
    }

    private void runWait(Map<String, Object> script) throws Exception {
        int waittimeout = 30;
        if(script.containsKey("timeout")){
            waittimeout = Integer.parseInt(script.get("timeout").toString());
        }
        WebDriverWait wait = new WebDriverWait(driver, waittimeout);

        wait.until(ExpectedConditions.visibilityOfElementLocated(ByElement(script.get("selector").toString(), script.get("name").toString())));

    }

    private void captureList(Map<String, Object> script){

    }

    private void loop(Map<String, Object> script) throws Exception {
        String loopType = script.get("type").toString();
        if(loopType.equals("variable")){
            Map<String, Object> subscripts = (Map<String, Object>) masterScript.get("subscripts");
            Map<String, Object> subscript = (Map<String, Object>) subscripts.get(script.get("subscript"));
            runScript(subscript);
        }
    }

    private void click(Map<String, Object> script) throws Exception {
        WebElement element = selectElement(script.get("selector").toString(), script.get("name").toString());
        element.click();
    }

    private void clickListItem(Map<String, Object> script) throws Exception {
        List<WebElement> element = selectElements(script.get("selector").toString(), script.get("name").toString());
        element.get(Integer.parseInt(script.get("item").toString())).click();
    }

    private void snapshot(){
        snapshots.add(driver.getPageSource());
    }




}
