package com.kytheralabs;

import java.text.ParseException;
import java.util.*;
import java.io.File;
import java.io.IOException;
import org.openqa.selenium.By;
import org.openqa.selenium.*;
import org.openqa.selenium.Keys;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.support.ui.*;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Selenium Scripter, generate selenium scripts from YAML.
 */
public class SeleniumScripter {
    // Variables
    private Object loopValue;
    private Map<String, Object> masterScript;

    // Constants
    private final WebDriver driver;
    private final FluentWait<WebDriver> wait;
    private final FluentWait<WebDriver> waits;
    private final List<String> snapshots = new ArrayList<>();
    private final Map<String, List> captureLists = new HashMap<>();

    public SeleniumScripter(WebDriver webDriver){
        driver = webDriver;
        wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(180))
                .pollingEvery(Duration.ofSeconds(5))
                .ignoring(NoSuchElementException.class).ignoring(ElementClickInterceptedException.class);
        waits = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofSeconds(5))
                .ignoring(NoSuchElementException.class).ignoring(ElementClickInterceptedException.class);
    }

    /**
     * Find the position of the first matching string in the set.
     * @param match search for this
     * @param set search across this
     * @return int the position in the set
     */
    private int firstMatch(String match, String[] set) {
        for(int i = 0; i < set.length; i++) {
            if(set[i].equals(match)) {
                return i;
            }
        }
        throw new IndexOutOfBoundsException("Instruction name does not exist: " + match);
    }

    /**
     * Run a selenium script, see wiki for more details.
     * @param script The Yaml() object containing the script
     * @throws IOException when a snapshot image failed to save to disk
     * @throws ParseException when a parsing error was found in the script
     * @throws InterruptedException when the process wakes up from a sleep event
     */
    public boolean runScript(Map<String, Object> script) throws IOException, ParseException, InterruptedException {
        return runScript(script, null);
    }

    /**
     * Run a selenium script, see wiki for more details.
     * @param script The Yaml() object containing the script
     * @param loopValue The set of the script to perform
     * @throws IOException when a snapshot image failed to save to disk
     * @throws ParseException when a parsing error was found in the script
     * @throws InterruptedException when the process wakes up from a sleep event
     */
    public boolean runScript(Map<String, Object> script, Object loopValue) throws IOException, ParseException, InterruptedException {
        boolean success = false;
        System.out.println("Processing Selenium Script");
        System.out.println("Objects found: " + script.size());
        this.loopValue = loopValue;
        if(masterScript == null){
            masterScript = script;
        }

        int position;
        String[] instructionNames = script.keySet().toArray(new String[0]);

        try {
            for (position = 0; position < instructionNames.length; position++) {
                String instructionName = instructionNames[position];
                Object instructionBlock = script.get(instructionName);

                System.out.println("Key: " + instructionName + " & Value: " + instructionBlock);
                if (instructionBlock instanceof Map) {
                    Map<String, Object> subscript = (Map<String, Object>) instructionBlock;
                    String operation = subscript.getOrDefault("operation", "{UNDEFINED}")
                                                .toString()
                                                .toLowerCase();

                    switch(operation.toLowerCase()) {
                        case "capturelist" -> captureList(subscript);
                        case "click" -> click(subscript);
                        case "clicklistitem" -> clickListItem(subscript);
                        case "if" -> {
                            // """"Validation""""
                            String[] required = {"condition", "then"};
                            for (String r : required){
                                if(!subscript.containsKey(r)) {
                                    throw new ParseException("`if` block must specify `" + r + "` parameter!", 0);
                                }
                            }

                            // Start processing the logic block
                            boolean willJump = false;
                            Map<String, String> conditionBody = (Map<String, String>) subscript.get("condition");
                            Map<String, Object> condition = new HashMap<>();
                            condition.put("condition", conditionBody);
                            String thenInstructionName = subscript.get("then").toString();
                            String elseInstructionName = subscript.getOrDefault("else", "n/a").toString();

                            if (runScript(condition)) {
                                position = firstMatch(thenInstructionName, instructionNames);
                                willJump = true;
                            } else if (!elseInstructionName.equalsIgnoreCase("n/a")) {
                                position = firstMatch(elseInstructionName, instructionNames);
                                willJump = true;
                            }

                            if (willJump) {
                                position -= 1; // Subtract 1, because the for loop is about to re-increment
                                String newName = instructionNames[position + 1];
                                System.err.println("Jumping to instruction: \"" + newName + "\"");
                            } else {
                                System.err.println("Condition did not meet, and no `else` clause was specified! Falling through...");
                            }
                        }
                        case "jsclick" -> jsclicker(subscript);
                        case "select" -> select(subscript);
                        case "{undefined}" -> System.err.println("Found the " + instructionName + " block with no defined operation! Skipping...");
                        case "keys" -> sendKeys(subscript);
                        case "loop" -> loop(subscript);
                        case "screenshot" -> screenshot(subscript);
                        case "snapshot" -> snapshot();
                        case "table" -> iterateTable(subscript);
                        case "wait" -> wait(subscript);
                        default -> throw new ParseException("Invalid operation: " + operation, 0);
                    }
                }
            }

            success = true;
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }

        System.out.println("SNAPSHOTS TAKEN: " + snapshots.size());
        return success;
    }

    /**
     * Take a screenshot for debugging purposes
     * @param script the screenshot subscript operation
     * @throws IOException when a screenshot image fails to write to disk
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
     * @param script the itterate-table subscript operation
     * @throws IOException when a snapshot image failed to save to disk
     * @throws ParseException when a parsing error was found in the script
     * @throws InterruptedException when the process wakes up from a sleep event
     */
    private void iterateTable(Map<String, Object> script) throws IOException, ParseException, InterruptedException {
        int offset = Integer.parseInt(script.getOrDefault("rowoffset", "0").toString());

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
                runScript(subscript, null);
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
                    runScript(subscript, null);
                } catch(org.openqa.selenium.NoSuchElementException e){
                    System.out.println("Can't find next button, exiting loop");
                    break;
                }
            } else {
                System.out.println("Now more rows left to parse");
                break;
            }
        }
    }


    /**
     * Select a single element.
     * @param selector the method of HTML element selection
     * @param name the attribute value of the element to select
     * @return WebElement
     */
    private WebElement selectElement(String selector, String name) {
        return switch (selector) {
            case "id" -> driver.findElement(By.id(name));
            case "class" -> driver.findElement(By.className(name));
            case "cssSelector" -> driver.findElement(By.cssSelector(name));
            case "xpath" -> driver.findElement(By.xpath(name));
            case "name" -> driver.findElement(By.name(name));
            default -> throw new NotFoundException("Could not find element with " + selector + " of " + name);
        };
    }

    /**
     * Select multiple elements.
     * @param selector the method of HTML element selection
     * @param name the attribute value of the element to select
     * @return List<WebElement>
     */
    private List<WebElement> selectElements(String selector, String name) {
        return switch (selector) {
            case "id" -> driver.findElements(By.id(name));
            case "class" -> driver.findElements(By.className(name));
            case "cssSelector" -> driver.findElements(By.cssSelector(name));
            case "xpath" -> driver.findElements(By.xpath(name));
            case "name" -> driver.findElements(By.name(name));
            default -> throw new NotFoundException("Could not find element with " + selector + " of " + name);
        };
    }

    /**
     * Get a By Element object
     * @param selector the HTML selection method
     * @param name the value of the selection attribute
     * @return By the desired element
     */
    private By ByElement(String selector, String name) {
        return switch (selector) {
            case "id" -> By.id(name);
            case "class" -> By.className(name);
            case "cssSelector" -> By.cssSelector(name);
            case "xpath" -> By.xpath(name);
            case "name" -> By.name(name);
            default -> null;
        };
    }

    /**
     * Interact with a select object on a webpage.
     * @param script the selection subscript operation
     */
    private void select(Map<String, Object> script) {
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
     * @param script the key-entry subscript operation
     * @throws InterruptedException interruption signal that occurs after sleeping
     */
    private void sendKeys(Map<String, Object> script) throws InterruptedException {
        WebElement element = selectElement(script.get("selector").toString(), script.get("name").toString());
        String input = script.get("value").toString().toLowerCase();

        switch (input) {
            case "{enter}" -> element.sendKeys(Keys.ENTER);
            case "{return}" -> element.sendKeys(Keys.RETURN);
            case "{down}" -> element.sendKeys(Keys.ARROW_DOWN);
            default -> { // If input is none of the keywords then slow-type the input
                // Convert the input to loop-value if it's said keyword
                input = (input.equals("${loopvalue}")) ? this.loopValue.toString() : input;

                // Clear the input field
                element.clear();

                // Slow-type each character
                for (char s : input.toCharArray()) {
                    System.out.println("Inserting: " + String.valueOf(s));
                    element.sendKeys(String.valueOf(s));
                    Thread.sleep(300);
                }

                Thread.sleep(5000); // Wait even more for some reason?
            }
        }
    }

    /**
     * Wait for an element to become visible.
     * @param script the wait subscript operation
     * @throws InterruptedException interruption signal that occurs after sleeping
     */
    private void wait(Map<String, Object> script) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Number timeout = 30;

        if (script.containsKey("timeout")) {
            String rawTimeout = script.get("timeout").toString();
            Scanner typeChecker = new Scanner(rawTimeout);

            if(typeChecker.hasNextDouble()){
                timeout = Double.parseDouble(rawTimeout);
            } else if(typeChecker.hasNextInt()) {
                timeout = Integer.parseInt(rawTimeout);
            }
        }

        if (!script.containsKey("selector")) {
            long delay = timeout.longValue() * 1000L;
            System.out.println("Sleeping for " + delay + " milliseconds!");
            Thread.sleep(delay);
        } else {
            System.out.println("Waiting for object: " + script.get("name").toString());
            //new WebDriverWait(driver, waittimeout)


            if (timeout.intValue() == 180) {
                wait.until(ExpectedConditions.visibilityOfElementLocated(ByElement(script.get("selector").toString(), script.get("name").toString())));
            } else {
                waits.until(ExpectedConditions.visibilityOfElementLocated(ByElement(script.get("selector").toString(), script.get("name").toString())));
            }

            System.out.println("Object found");
          
            if (script.containsKey("asyncwait") && script.get("asyncwait").equals(true)) {
                //To set the script timeout to 10 seconds
                driver.manage().timeouts().setScriptTimeout(30, TimeUnit.SECONDS);
                //To declare and set the start time
                long startTime = System.currentTimeMillis();
                //Calling executeAsyncScript() method to wait for js
                js.executeAsyncScript("window.setTimeout(arguments[arguments.length - 1], 20000);");
                //To get the difference current time and start time
                System.out.println("Wait time: " + (System.currentTimeMillis() - startTime));
            }
        }
    }

    /**
     * Create a capture list. A capture list is a list of elements or labels which you can iterate over elsewhere in your script.
     * @param script the capture-list subscript operation
     */
    private void captureList(Map<String, Object> script) {
        System.out.println("Generating Capture List");
        List<WebElement> webElements = selectElements(script.get("selector").toString(), script.get("name").toString());
        String type = "text";
        if(script.containsKey("collect")){
            type = script.get("collect").toString();
        }
        if(script.containsKey("type")){
            type = script.get("type").toString();
        }
        List strlist = new ArrayList<>();
        for(WebElement el : webElements){
            System.out.println("Capture Element Found: "+el.getText());
            switch (type) {
                case "text" -> strlist.add(el.getText());
                case "elements" -> strlist.add(el);
                case "xpath" -> strlist.add(getElementXPath(el));
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
     * @param element the element of focus
     * @return String the full xpath
     */
    public String getElementXPath(WebElement element) {
        return (String)((JavascriptExecutor) driver).executeScript("gPt=function(c){if(c.id!==''){return'[@id=\"'+c.id+'\"]'}if(c===document.body){return c.tagName}var a=0;var e=c.parentNode.childNodes;for(var b=0;b<e.length;b++){var d=e[b];if(d===c){return gPt(c.parentNode)+'/'+c.tagName+'['+(a+1)+']'}if(d.nodeType===1&&d.tagName===c.tagName){a++}}};return gPt(arguments[0]);", element);
    }

    /**
     * Loop over a variable and run a script on each iteration.
     * @param script the loop subscript operation
     */
    private void loop(Map<String, Object> script) {
        String loopType = script.get("type").toString();
        List<String> vars = captureLists.get(script.get("variable").toString());
        if(loopType.equals("variable")) {
            System.out.println("Performing Variable Loop for: " + script.get("variable").toString());
            for (Object v : vars) {
                Map<String, Object> subscripts = (Map<String, Object>) masterScript.get("subscripts");
                Map<String, Object> subscript = (Map<String, Object>) subscripts.get(script.get("subscript"));
                System.out.println("Looping for variable: " + v+ " . Using subscript: "+ script.get("subscript"));
                try {
                    runScript(subscript, v);
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
     * @param script the click subscript operation
      */
    private void click(Map<String, Object> script) {
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
     * Uses JavascriptExecutor to click a button
     * @param script the jsClick subscript operation
     */
    private void jsclicker(Map<String, Object> script) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement element =null;
        if(script.containsKey("variable") && script.get("variable").equals(true)){
            String n = script.get("name").toString().replace("{variable}", this.loopValue.toString());
            element = selectElement(script.get("selector").toString(), n);
        } else{
            element = selectElement(script.get("selector").toString(), script.get("name").toString());
        }

        if(element != null){
            System.out.println("Clicking Element");
            js.executeScript("arguments[0].click();", element);
        }else{
            if(script.containsKey("back") && script.get("back").equals(true)){
                try{
                    System.out.println("Going to last page");
                    //Calling executeAsyncScript() method to go back a page
                    js.executeScript("window.history.go(-1);");
                    //waits for page to load
                    js.executeAsyncScript("window.setTimeout(arguments[arguments.length - 1], 10000);");
                    System.out.println("Page refreshed");
                } catch (org.openqa.selenium.NoSuchElementException e){
                    System.out.println("Element not found but continuing.");
                }
            }else{
                System.out.println("Element null, nothing to click.");

            }
        }
    }

    /**
     * Click on an item in a list.
     * @param script the click-list-item subscript operation
     */
    private void clickListItem(Map<String, Object> script) {
        List<WebElement> element = selectElements(script.get("selector").toString(), script.get("name").toString());
        int i = ((Double) script.get("item")).intValue();
        System.out.println("Clicking list item");
        element.get(i).click();
    }

    /**
     * Take a snapshot and store the HTML content on the page.
     */
    private void snapshot() {
        System.out.println("Taking Snapshot");
        snapshots.add(driver.getPageSource());
    }

    /**
     * Return the snapshots
     * @return List<String> the list of paths to snapshot images taken
     */
    public List<String> getSnapshots(){
        return this.snapshots;
    }
}
