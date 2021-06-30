package com.kytheralabs;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    private static final Logger LOG = LogManager.getLogger(SeleniumScripter.class);

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

    private void validate(Map<String, Object> script, String requiredField) throws ParseException {
        validate(script, new String[] {requiredField});
    }

    private void validate(Map<String, Object> script, String[] requiredFields) throws ParseException {
        for (String r : requiredFields) {
            if (!script.containsKey(r)) {
                throw new ParseException("Expected `" + r + "` field in block: `" + script + "`, but none was found!", 0);
            }
        }
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
        LOG.info("Processing Selenium Script");
        LOG.info("Objects found: " + script.size());

        this.loopValue = loopValue;
        if(masterScript == null){
            masterScript = script;
        }

        try {
            for (Map.Entry instruction : script.entrySet()) {
                String instructionName = instruction.getKey().toString();
                Object instructionBlock = instruction.getValue();

                LOG.info("Key: " + instructionName + " & Value: " + instructionBlock);
                if (instructionBlock instanceof Map) {
                    Map<String, Object> subscript = (Map<String, Object>) instructionBlock;
                    String operation = subscript.getOrDefault("operation", "{UNDEFINED}")
                                                .toString()
                                                .toLowerCase();

                    switch (operation.toLowerCase()) {
                        case "capturelist":
                            captureList(subscript);
                            break;
                        case "click":
                            click(subscript);
                            break;
                        case "clicklistitem":
                            clickListItem(subscript);
                            break;
                        case "if":
                            ifBlock(subscript);
                            break;
                        case "injectcontent":
                            injectContent(subscript);
                            break;
                        case "jsclick":
                            jsClicker(subscript);
                            break;
                        case "jsback":
                            jsBack(subscript);
                            break;
                        case "jsrefresh":
                            jsRefresh(subscript);
                            break;
                        case "select":
                            selectDropdown(subscript);
                            break;
                        case "{undefined}":
                            System.err.println("Found the " + instructionName + " block with no defined operation! Skipping...");
                            break;
                        case "keys":
                            sendKeys(subscript);
                            break;
                        case "loop":
                            loop(subscript);
                            break;
                        case "screenshot":
                            screenshot(subscript);
                            break;
                        case "snapshot":
                            snapshot();
                            break;
                        case "table":
                            iterateTable(subscript);
                            break;
                        case "wait":
                            wait(subscript);
                            break;
                        default:
                            throw new ParseException("Invalid operation: " + operation, 0);
                    }
                }
                else {
                    throw new ParseException("Subscript did not convert to map!", 0);
                }
            }

            success = true;
        } catch (NoSuchElementException | TimeoutException e) {
            e.printStackTrace();
        }

        LOG.info("SNAPSHOTS TAKEN: " + snapshots.size());
        return success;
    }

    /**
     * Take a screenshot for debugging purposes
     * @param script the screenshot subscript operation
     * @throws IOException when a screenshot image fails to write to disk
     */
    public void screenshot(Map<String, Object> script) throws IOException, ParseException {
        validate(script, "type"); // Validation
        TakesScreenshot scrShot =((TakesScreenshot)driver);

        if(script.get("type").equals("file")) {
            File f = scrShot.getScreenshotAs(OutputType.FILE);
            File dest = new File((String) script.get("targetdir"));
            FileUtils.copyFile(f, dest);
        } else if(script.get("type").equals("dbfs")){
        }
    }

    /**
     * Process a logical `if` block
     * @param script
     * @throws ParseException
     * @throws IOException
     * @throws InterruptedException
     */
    private void ifBlock(Map<String, Object> script) throws ParseException,
                                                            IOException,
                                                            InterruptedException {
        validate(script, new String[] {"condition", "then"}); // Validation

        // Fetch the instruction blocks
        Map<String, String> conditionBody = (Map<String, String>) ((ArrayList)script.get("condition")).get(0);
        List<Map<String, String>> thenBody = (List<Map<String, String>>) script.get("then");
        List<Map<String, String>> elseBody = (List<Map<String, String>>) script.get("else");

        // Prepare the condition block
        Map<String, Object> condition = new HashMap<>();
        condition.put("condition", conditionBody);

        if (runScript(condition)) {
            LOG.debug("Processing `then` block with " + thenBody.size() + " items!");
            for(Map<String, String> thenSubBlock: thenBody){
                Map<String, Object> thenBlock = new HashMap<>();
                thenBlock.put("else", thenSubBlock);

                runScript(thenBlock);
            }
        } else if (elseBody != null) {
            LOG.debug("Processing `else` block with " + thenBody.size() + " items!");
            for(Map<String, String> elseSubBlock: elseBody){
                Map<String, Object> elseBlock = new HashMap<>();
                elseBlock.put("else", elseSubBlock);

                runScript(elseBlock);
            }
        }
        else {
            LOG.debug("Condition did not meet, and no `else` clause was specified! Falling through...");
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
        // validate(script, new String[] {""}); // Validation

        int offset = Integer.parseInt(script.getOrDefault("rowoffset", "0").toString());
        while (true) {
            List<WebElement>  allRows = selectElements(script.get("selector").toString(), script.get("name").toString());
            int elementcount = allRows.size();

            LOG.debug("Found " + elementcount + " rows in table!");

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
                LOG.debug("Waiting for object: "+script.get("name").toString());
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
                    LOG.info("Can't find next button, exiting loop");
                    break;
                }
            } else {
                LOG.debug("Now more rows left to parse");
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
        switch (selector) {
            case "id":
                return driver.findElement(By.id(name));
            case "class":
                return driver.findElement(By.className(name));
            case "cssSelector":
                return driver.findElement(By.cssSelector(name));
            case "xpath":
                return driver.findElement(By.xpath(name));
            case "name":
                return driver.findElement(By.name(name));
            default:
                throw new NotFoundException("Could not find element with " + selector + " of " + name);
        }
    }

    /**
     * Select multiple elements.
     * @param selector the method of HTML element selection
     * @param name the attribute value of the element to select
     * @return List<WebElement>
     */
    private List<WebElement> selectElements(String selector, String name) {
        switch (selector) {
            case "id":
                return driver.findElements(By.id(name));
            case "class":
                return driver.findElements(By.className(name));
            case "cssSelector":
                return driver.findElements(By.cssSelector(name));
            case "xpath":
                return driver.findElements(By.xpath(name));
            case "name":
                return driver.findElements(By.name(name));
            default:
                throw new NotFoundException("Could not find element with " + selector + " of " + name);
        }
    }

    /**
     * Get a By Element object
     * @param selector the HTML selection method
     * @param name the value of the selection attribute
     * @return By the desired element
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
            default:
                return null;
        }
    }

    /**
     * Interact with a select object on a webpage.
     * @param script the selection subscript operation
     */
    private void selectDropdown(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"selector", "name"}); // Validation

        WebElement element = selectElement(script.get("selector").toString(), script.get("name").toString());
        Select selectObj = new Select(element);
        if(script.get("selectBy").equals("value")){
            LOG.info("Run select by value");
            selectObj.selectByValue(script.get("value").toString());
        } else if(script.get("selectBy").equals("index")){
            LOG.info("Run select by index");
            selectObj.selectByIndex(((Double) script.get("value")).intValue());
        } else if(script.get("selectBy").equals("visible")){
            LOG.info("Run select by visible text");
            selectObj.selectByVisibleText(script.get("value").toString());
        }
    }

    /**
     * Type some keys into your website.
     * @param script the key-entry subscript operation
     * @throws InterruptedException interruption signal that occurs after sleeping
     */
    private void sendKeys(Map<String, Object> script) throws InterruptedException, ParseException {
        validate(script, new String[] {"selector", "name", "value"}); // Validation

        WebElement element = selectElement(script.get("selector").toString(), script.get("name").toString());
        String input = script.get("value").toString().toLowerCase();

        if ("{enter}".equals(input)) {
            element.sendKeys(Keys.ENTER);
        } else if ("{return}".equals(input)) {
            element.sendKeys(Keys.RETURN);
        } else if ("{down}".equals(input)) {
            element.sendKeys(Keys.ARROW_DOWN);
        } else { // If input is none of the keywords then slow-type the input
            // Convert the input to loop-value if it's said keyword
            input = (input.equals("${loopvalue}")) ? this.loopValue.toString() : input;

            // Clear the input field
            element.clear();

            // Slow-type each character
            for (char s : input.toCharArray()) {
                LOG.info("Inserting: " + String.valueOf(s));
                element.sendKeys(String.valueOf(s));
                Thread.sleep(300);
            }

            if(script.containsKey("wait")){
                String rawTimeout = script.get("wait").toString();
                Scanner typeChecker = new Scanner(rawTimeout);

                Number timeout = 0;
                if(typeChecker.hasNextDouble()){
                    timeout = Double.parseDouble(rawTimeout);
                } else if(typeChecker.hasNextInt()) {
                    timeout = Integer.parseInt(rawTimeout);
                }
              Thread.sleep(timeout.longValue());
            } else {
                Thread.sleep(5000); // Wait even more for some reason?
            }
        }
    }

    /**
     * Wait for an element to become visible.
     * @param script the wait subscript operation
     * @throws InterruptedException interruption signal that occurs after sleeping
     */
    private void wait(Map<String, Object> script) throws InterruptedException, TimeoutException {
        // validate(script, new String[] {"selector", ""}); // Validation

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
            LOG.info("Sleeping for " + delay + " milliseconds!");
            Thread.sleep(delay);
        } else {
            if (timeout.intValue() == 180) {
                wait.until(ExpectedConditions.visibilityOfElementLocated(ByElement(script.get("selector").toString(), script.get("name").toString())));
            } else {
                waits.until(ExpectedConditions.visibilityOfElementLocated(ByElement(script.get("selector").toString(), script.get("name").toString())));
            }

            LOG.info("Object found");
          
            if (script.containsKey("asyncwait") && script.get("asyncwait").equals(true)) {
                //To set the script timeout to 10 seconds
                driver.manage().timeouts().setScriptTimeout(30, TimeUnit.SECONDS);
                //To declare and set the start time
                long startTime = System.currentTimeMillis();
                //Calling executeAsyncScript() method to wait for js
                js.executeAsyncScript("window.setTimeout(arguments[arguments.length - 1], 20000);");
                //To get the difference current time and start time
                LOG.info("Wait time: " + (System.currentTimeMillis() - startTime));
            }
        }
    }

    /**
     * Injects content onto the stack. By default, the content is an error message indicating token info was not found.
     * @param script the inject-content subscript instruction
     * @throws ParseException occurs when the `type` block is not specified or when an invalid type is specified
     */
    private void injectContent(Map<String, Object> script) throws ParseException {
        validate(script, "type"); // Validation

        String content;
        final String type = script.get("type").toString().toLowerCase();
        final String tokenName = script.getOrDefault("name", "null").toString();

        switch (type) {
            case "override":
                validate(script, "value");
                content = script.get("value").toString();
                break;
            case "html":
                content = "<p id=\"error\">no results found</p><p id=\"token\">" + tokenName + "</p>";
                break;
            case "json":
                content = "{\"error\": \"no results found\", \"token\": \"" + tokenName + "\"}";
                break;
            default:
                throw new ParseException("Invalid `type`: " + type, 0);
        }

        LOG.warn("Injecting " + type + " content onto stack: `" + content + "`");
        snapshots.add(content);
    }

    /**
     * Create a capture list. A capture list is a list of elements or labels which you can iterate over elsewhere in your script.
     * @param script the capture-list subscript operation
     */
    private void captureList(Map<String, Object> script) {
        // validate(script, new String[] {""}); // Validation

        LOG.info("Generating Capture List");

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
            LOG.info("Capture Element Found: "+el.getText());
            if ("text".equals(type)) {
                strlist.add(el.getText());
            } else if ("elements".equals(type)) {
                strlist.add(el);
            } else if ("xpath".equals(type)) {
                strlist.add(getElementXPath(el));
            }
        }

        LOG.info("Storing capture list as: "+script.get("variable").toString());
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
        return (String) ((JavascriptExecutor) driver).executeScript("gPt=function(c){if(c.id!==''){return'[@id=\"'+c.id+'\"]'}if(c===document.body){return c.tagName}var a=0;var e=c.parentNode.childNodes;for(var b=0;b<e.length;b++){var d=e[b];if(d===c){return gPt(c.parentNode)+'/'+c.tagName+'['+(a+1)+']'}if(d.nodeType===1&&d.tagName===c.tagName){a++}}};return gPt(arguments[0]);", element);
    }

    /**
     * Loop over a variable and run a script on each iteration.
     * @param script the loop subscript operation
     */
    private void loop(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"type", "variable"}); // Validation

        String loopType = script.get("type").toString();
        List<String> vars = captureLists.get(script.get("variable").toString());
        if(loopType.equals("variable")) {
            LOG.info("Performing Variable Loop for: " + script.get("variable").toString());
            for (Object v : vars) {
                Map<String, Object> subscripts = (Map<String, Object>) masterScript.get("subscripts");
                Map<String, Object> subscript = (Map<String, Object>) subscripts.get(script.get("subscript"));
                LOG.info("Looping for variable: " + v+ " . Using subscript: "+ script.get("subscript"));
                try {
                    runScript(subscript, v);
                } catch (Exception e){
                    LOG.error(e);
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
        // validate(script, new String[] {""}); // Validation

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
                    LOG.info("Element not found but continuing.");
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
            LOG.info("Clicking Element");
            element.click();
        } else{
            LOG.info("Element null, nothing to click.");
        }
    }

    /**
     * Uses JavascriptExecutor to click a button
     * @param script the jsClick subscript operation
     */
    private void jsClicker(Map<String, Object> script) {
        // validate(script, new String[] {""}); // Validation

        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement element =null;
        if(script.containsKey("variable") && script.get("variable").equals(true)){
            String n = script.get("name").toString().replace("{variable}", this.loopValue.toString());
            element = selectElement(script.get("selector").toString(), n);
        } else{
            element = selectElement(script.get("selector").toString(), script.get("name").toString());
        }

        if(element != null){
            LOG.info("Clicking Element");
            js.executeScript("arguments[0].click();", element);
        }else{
            if(script.containsKey("back") && script.get("back").equals(true)){
                try{
                    LOG.info("Going to last page");
                    //Calling executeAsyncScript() method to go back a page
                    js.executeScript("window.history.go(-1);");
                    //waits for page to load
                    js.executeAsyncScript("window.setTimeout(arguments[arguments.length - 1], 10000);");
                    LOG.info("Page refreshed");
                } catch (org.openqa.selenium.NoSuchElementException e){
                    LOG.info("Element not found but continuing.");
                }
            } else{
                LOG.info("Element null, nothing to click.");
            }
        }
    }

    private void jsBack(Map<String, Object> script) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        if(script.containsKey("back") && script.get("back").equals(true)) {
            try {
                LOG.info("Going to last page");
                //Calling executeAsyncScript() method to go back a page
                js.executeScript("window.history.back();");
                //waits for page to load
                js.executeAsyncScript("window.setTimeout(arguments[arguments.length - 1], 10000);");
                LOG.info("Page refreshed");
            } catch (org.openqa.selenium.NoSuchElementException e) {
                LOG.info("Back operation failed.");
            }
        }
    }

    private void jsRefresh(Map<String, Object> script) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        if (script.containsKey("refresh") && script.get("refresh").equals(true)) {
            try {
                LOG.info("Refreshing the page");
                //Calling executeAsyncScript() method to go back a page
                js.executeScript("location.reload();");
                //waits for page to load
                js.executeAsyncScript("window.setTimeout(arguments[arguments.length - 1], 10000);");
            } catch (org.openqa.selenium.NoSuchElementException e) {
                LOG.info("Refresh failed");
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
        LOG.info("Clicking list item");
        element.get(i).click();
    }

    /**
     * Take a snapshot and store the HTML content on the page.
     */
    private void snapshot() {
        LOG.info("Taking Snapshot");
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
