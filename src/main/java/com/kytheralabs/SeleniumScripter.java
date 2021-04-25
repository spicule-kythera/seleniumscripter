package com.kytheralabs;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Selenium Scripter, generate selenium scripts from YAML.
 */
public class SeleniumScripter {
    private final FluentWait<WebDriver> waits;
    private final FluentWait<WebDriver> wait;
    private WebDriver driver;
    private Map<String, Object> masterScript;
    private final Map<String, List> captureLists = new HashMap<>();
    private final List<String> snapshots = new ArrayList<>();
    private Object loopValue;

    public SeleniumScripter(WebDriver webDriver){
        driver = webDriver;
        wait = new FluentWait<WebDriver>(driver)
                .withTimeout(180, SECONDS)
                .pollingEvery(5, SECONDS)
                .ignoring(NoSuchElementException.class).ignoring(ElementClickInterceptedException.class);
        waits = new FluentWait<WebDriver>(driver)
                .withTimeout(10, SECONDS)
                .pollingEvery(5, SECONDS)
                .ignoring(NoSuchElementException.class).ignoring(ElementClickInterceptedException.class);
    }

    /**
     * Run a selenium script, see wiki for more details.
     * @param script
     * @param iteration
     * @param loopValue
     * @throws Exception
     */
    public void runScript(Map<String, Object> script, Integer iteration, Object loopValue) throws Exception {
        System.out.println("Processing Selenium Script");
        System.out.println("Objects found: "+script.size());
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
                    } else if (obj.get("operation").equals("screenshot")) {
                        screenshot(obj);
                    } else if (obj.get("operation").equals("snapshot")) {
                        snapshot();
                    } else if (obj.get("operation").equals("table")){
                        iterateTable(obj);
                    } else if (obj.get("operation").equals("conditional")){
                        conditionalCheck(obj);
                    }
                }
            }

        }

        System.out.println("SNAPSHOTS TAKEN: "+snapshots.size());
    }

    private void conditionalCheck(Map<String, Object> obj) {
    }

    /**
     * Take a screenshot for debugging purposes
     * @param script
     * @throws IOException
     */
    public void screenshot(Map<String, Object> script) throws IOException {
        TakesScreenshot scrShot =((TakesScreenshot)driver);

        if(script.get("type").equals("file")) {
            File f = scrShot.getScreenshotAs(OutputType.FILE);
            File dest = new File((String) script.get("targetdir"));
            FileUtils.copyFile(f, dest);
        } else if(script.get("type").equals("dbfs")){
        }
    }

    /**
     * Iterate through a tables rows and perform a subscript on each row.
     * @param script
     * @throws Exception
     */
    private void iterateTable(Map<String, Object> script) throws Exception {
        int offset = 0;
        if(script.containsKey("rowoffset")){
            offset = ((Double) script.get("rowoffset")).intValue();
        }
        while (true) {
            List<WebElement>  allRows = selectElements(script.get("selector").toString(), script.get("name").toString());
            int elementcount = allRows.size();
            System.out.println("ROWS FOUND IN TABLE: "+ elementcount);
            if(elementcount <= offset){
                break;
            }
            for (int i = offset; i < elementcount; i++) {
                String xpath = script.get("name").toString();
                xpath = xpath + "[" + i + "]";
                allRows = selectElements(script.get("selector").toString(), xpath);
                allRows.get(0).click();
                Map<String, Object> subscripts = (Map<String, Object>) masterScript.get("subscripts");
                Map<String, Object> subscript = (Map<String, Object>) subscripts.get(script.get("subscript"));
                runScript(subscript, null, null);
                WebDriverWait wait = new WebDriverWait(driver, 180);
                System.out.println("Waiting for object: "+script.get("name").toString());
                wait.until(ExpectedConditions.visibilityOfElementLocated(ByElement(script.get("selector").toString(), script.get("name").toString())));
            }
            if (script.containsKey("nextbuttonscript")) {
                Map<String, Object> subscripts = (Map<String, Object>) masterScript.get("subscripts");
                Map<String, Object> subscript = (Map<String, Object>) subscripts.get(script.get("nextbuttonscript"));
                Map<String, Object> nextbuttonAttrs = (Map<String, Object>) script.get("nextbutton");
                try{
                    selectElement(nextbuttonAttrs.get("selector").toString(), nextbuttonAttrs.get("name").toString());
                    runScript(subscript, null, null);
                } catch(org.openqa.selenium.NoSuchElementException e){
                    System.out.println("Can't find next button, exiting loop");
                    break;
                }
            } else{
                System.out.println("Now more rows left to parse");
                break;
            }
        }
    }


    /**
     * Select an element by selector and nmme.
     * @param selector
     * @param name
     * @return
     * @throws Exception
     */
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

    /**
     * Select multiple elements by selector and name.
     * @param selector
     * @param name
     * @return
     * @throws Exception
     */
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

    /**
     * Get a By Element object
     * @param selector
     * @param name
     * @return
     */
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

    /**
     * Interact with a select object on a webpage.
     * @param script
     * @throws Exception
     */
    private void runSelect(Map<String, Object> script) throws Exception {
        WebElement element = selectElement(script.get("selector").toString(), script.get("name").toString());
        Select selectObj = new Select(element);
        if(script.get("selectBy").equals("value")){
            System.out.println("Run select by value");
            selectObj.selectByValue(script.get("value").toString());
        } else if(script.get("selectBy").equals("index")){
            System.out.println("Run select by index");
            selectObj.selectByIndex(((Double) script.get("value")).intValue());
        } else if(script.get("selectBy").equals("visible")){
            System.out.println("Run select by visible text");
            selectObj.selectByVisibleText(script.get("value").toString());
        }
    }

    /**
     * Type some keys into your website.
     * @param script
     * @throws Exception
     */
    private void runKeys(Map<String, Object> script) throws Exception {
        WebElement element = selectElement(script.get("selector").toString(), script.get("name").toString());
        String keystring = script.get("value").toString();
        String target = keystring;
//        if(this.loopValue != null && !(this.loopValue instanceof String)) {
//            throw new Exception("Can't insert keys, value not string");
//
//        }

        if (keystring.equals("${loopvalue}")) {
            target = this.loopValue.toString();
        } else if(keystring.equals("{enter}")){
            element.sendKeys(Keys.ENTER);
            return;
        }else if(keystring.equals("{return}")){
            element.sendKeys(Keys.RETURN);
            return;
        }else if(keystring.equals("{down}")){
            element.sendKeys(Keys.ARROW_DOWN);
            return;
        }
        element.clear();
        for (char s : target.toCharArray()) {
            System.out.println("Inserting: " + String.valueOf(s));
            element.sendKeys(String.valueOf(s));
            Thread.sleep(300);
        }
        Thread.sleep(5000);

    }

    /**
     * Wait for an element to become visible.
     * @param script
     * @throws Exception
     */

    private void runWait(Map<String, Object> script) throws Exception {
        long waittimeout = 180;
        if(script.containsKey("timeout")){
            waittimeout = ((Double) script.get("timeout")).intValue();
            //waittimeout = (Integer) script.get("timeout");
        }

        if(script.get("selector").toString().equals("none")){
           // driver.manage().timeouts().implicitlyWait(waittimeout, SECONDS);
        } else {
            System.out.println("Waiting for object: " + script.get("name").toString());
            //new WebDriverWait(driver, waittimeout)


            if(waittimeout == 180){
                wait.until(ExpectedConditions.visibilityOfElementLocated(ByElement(script.get("selector").toString(), script.get("name").toString())));
            } else{
                waits.until(ExpectedConditions.visibilityOfElementLocated(ByElement(script.get("selector").toString(), script.get("name").toString())));
            }

            System.out.println("Object found");
        }
    }
    private void pause(Integer milliseconds){
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /**
     * Create a capture list. A capture list is a list of elements or labels which you can iterate over elsewhere in your script.
     * @param script
     * @throws Exception
     */
    private void captureList(Map<String, Object> script) throws Exception {
        System.out.println("Generating Capture List");
        List<WebElement> webElements = selectElements(script.get("selector").toString(), script.get("name").toString());
        String type = "text";
        if(script.containsKey("collect")){
            type = script.get("collect").toString();
        }
        List strlist = new ArrayList<>();
        for(WebElement el : webElements){
            System.out.println("Capture Element Found: "+el.getText());
            if(type.equals("text")){
                strlist.add(el.getText());
            } else if(type.equals("elements")){
                strlist.add(el);
            }

        }
        System.out.println("Storing capture list as: "+script.get("variable").toString());
        String append = "false";
        if(script.containsKey("append")){
            append = script.get("append").toString();
        }
        if(append.equals("false")) {
            captureLists.put(script.get("variable").toString(), strlist);
        } else if(append.equals("true")){
            List list = captureLists.get(script.get("variable"));
            List<String> newList = new ArrayList<String>(list);
            newList.addAll(strlist);
            captureLists.put(script.get("variable").toString(), newList);
        }
    }

    /**
     * Loop over a variable and run a script on each iteration.
     * @param script
     * @throws Exception
     */
    private void loop(Map<String, Object> script) throws Exception {
        String loopType = script.get("type").toString();
        if(loopType.equals("variable")){
            List<String> vars = captureLists.get(script.get("variable").toString());
            System.out.println("Performing Variable Loop for: "+script.get("variable").toString());
            for (Object v : vars) {
                Map<String, Object> subscripts = (Map<String, Object>) masterScript.get("subscripts");
                Map<String, Object> subscript = (Map<String, Object>) subscripts.get(script.get("subscript"));
                System.out.println("Looping for variable: " + v+ " . Using subscript: "+ script.get("subscript"));
                try {
                    runScript(subscript, null, v);
                } catch (Exception e){
                    if(!script.containsKey("exitOnError") || script.containsKey("exitOnError") && script.get("exitOnError").equals(true)){
                        break;
                    }
                }
            }
        }
    }

    /**
     * Click on an element on your website.
     * @param script
     * @throws Exception
     */
    private void click(Map<String, Object> script) throws Exception {
        WebElement element = null;
        if(script.containsKey("selector") && script.get("selector").equals("element")){
            if(script.containsKey("variable") && script.get("variable").equals("${inputElement}")){
                element = (WebElement) this.loopValue;
            }
        } else {
            if(script.containsKey("failNotFound") && script.get("failNotFound").equals(false)){
                try{
                    element = selectElement(script.get("selector").toString(), script.get("name").toString());
                } catch (org.openqa.selenium.NoSuchElementException e){
                    System.out.println("Element not found but continuing.");
                }
            } else {
                if(script.containsKey("variable") && script.get("variable").equals(true)){
                    String n = script.get("name").toString().replace("{variable}", this.loopValue.toString());
                    element = selectElement(script.get("selector").toString(), n);
                } else{
                    element = selectElement(script.get("selector").toString(), script.get("name").toString());
                }

            }
        }
        if(element != null) {
            System.out.println("Clicking Element");
            element.click();
        } else{
            System.out.println("Element null, nothing to click.");
        }
    }

    /**
     * Click on an item in a list.
     * @param script
     * @throws Exception
     */
    private void clickListItem(Map<String, Object> script) throws Exception {
        List<WebElement> element = selectElements(script.get("selector").toString(), script.get("name").toString());
        int i = ((Double) script.get("item")).intValue();
        System.out.println("Clicking list item");
        element.get(i).click();
    }

    /**
     * Take a snapshot and store the HTML content on the page.
     */
    private void snapshot(){
        System.out.println("Taking Snapshot");
        snapshots.add(driver.getPageSource());
    }

    /**
     * Return the snapshots
     * @return
     */
    public List<String> getSnapshots(){
        return this.snapshots;
    }

}
