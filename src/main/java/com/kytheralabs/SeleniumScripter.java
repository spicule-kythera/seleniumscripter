package com.kytheralabs;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Selenium Scripter, generate selenium scripts from YAML.
 */
public class SeleniumScripter {
    // Variables
    private Object loopValue;
    private Map<String, Object> masterScript;

    // Constants
    private final WebDriver driver; // The web driver
    private final String url;
    private final long defaultWaitTimeout = 30; // The default element wait timeout in seconds
    private final List<String> snapshots = new ArrayList<>(); // The stack of HTML content to return to the crawl
    private final Map<String, List> captureLists = new HashMap<>(); // Something?

    // Logger
    private static final Logger LOG = LogManager.getLogger(SeleniumScripter.class);

    public SeleniumScripter(WebDriver driver){
        this.driver = driver;
        url = driver.getCurrentUrl();
    }

    private Number parseNumber(String number) throws ParseException {
        Scanner scan = new Scanner(number);
        if(scan.hasNextInt()){
            return Integer.parseInt(number);
        }
        else if(scan.hasNextDouble()) {
            return Double.parseDouble(number);
        }
        else {
            throw new ParseException("Invalid numeric type: \"" + number + "\"", 0);
        }
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
     * Find a single web element. THIS IS AN UNSAFE FUNCTION, it does not guarantee existence or visibility.
     * @param selector the method of HTML element selection
     * @param name the attribute value of the element to select
     * @return WebElement
     */
    private WebElement findElement(String selector, String name) {
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
     * Find multiple web elements. THIS IS AN UNSAFE FUNCTION, it does not guarantee existence or visibility.
     * @param selector the method of HTML element selection
     * @param name the attribute value of the element to select
     * @return List<WebElement>
     */
    private List<WebElement> findElements(String selector, String name) {
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
     * Convert selector and value string to a `selenium.By` object
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
     * Fetch the absolute (unoptimized) xpath of a specified web element.
     * @param element the web element to fetch the path of
     * @return String the full web element xpath
     */
    public String getElementXPath(WebElement element) {
        return (String) ((JavascriptExecutor) driver).executeScript("gPt=function(c){if(c.id!==''){return'[@id=\"'+c.id+'\"]'}if(c===document.body){return c.tagName}var a=0;var e=c.parentNode.childNodes;for(var b=0;b<e.length;b++){var d=e[b];if(d===c){return gPt(c.parentNode)+'/'+c.tagName+'['+(a+1)+']'}if(d.nodeType===1&&d.tagName===c.tagName){a++}}};return gPt(arguments[0]);", element);
    }

    /**
     * Run a selenium script.
     *      A wrapper for `SeleniumScripter::runScript(script, loopValue) where loopValue is null.
     * @param script the serialized selenium script
     * @throws IOException occurs when a snapshot image failed to save to disk
     * @throws ParseException occurs when one of the required fields is missing
     * @throws InterruptedException occurs when the process wakes up from a sleep event in a child-instruction
     */
    public boolean runScript(Map<String, Object> script) throws IOException, ParseException, InterruptedException {
        return runScript(script, null);
    }

    /**
     * Run a selenium script.
     * @param script the serialized selenium script
     * @throws IOException occurs when a snapshot image failed to save to disk
     * @throws ParseException occurs when one of the required fields is missing
     * @throws InterruptedException occurs when the process wakes up from a sleep event in a child-instruction
     */
    public boolean runScript(Map<String, Object> script, Object loopValue) throws IOException,
                                                                                  ParseException,
                                                                                  InterruptedException {
        boolean success = false;
        LOG.info("Processing Selenium Script with " + script.size() + " objects!");

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
                        case "{undefined}":
                            LOG.warn("Found the " + instructionName + " block with no defined operation! Skipping...");
                            break;
                        case "capturelist":
                            captureListOperation(subscript);
                            break;
                        case "click":
                            clickOperation(subscript);
                            break;
                        case "clicklistitem":
                            clickListItemOperation(subscript);
                            break;
                        case "if":
                            ifOperation(subscript);
                            break;
                        case "injectcontent":
                            injectContentOperation(subscript);
                            break;
                        case "jsback":
                            jsBackOperation();
                            break;
                        case "jsclick":
                            jsClickOperation(subscript);
                            break;
                        case "jsrefresh":
                            jsRefreshOperation();
                            break;
                        case "keys":
                            keysOperation(subscript);
                            break;
                        case "loadpage":
                            loadPageOperation(subscript);
                            break;
                        case "loop":
                            loopOperation(subscript);
                            break;
                        case "restore":
                            restoreOperation(subscript);
                            break;
                        case "screenshot":
                            screenshotOperation(subscript);
                            break;
                        case "select":
                            selectOperation(subscript);
                            break;
                        case "snapshot":
                            snapshotOperation();
                            break;
                        case "table":
                            tableOperation(subscript);
                            break;
                        case "wait":
                            waitOperation(subscript);
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

        LOG.info(snapshots.size() + " snapshots taken at the end of this block!");
        return success;
    }

    /**
     * Create a capture list.
     *      A "captured list" is a list of elements or labels which can be iterated over, elsewhere.
     * @param script the capture-list subscript operation
     * @throws ParseException occurs when the required fields are not specified
     */
    private void captureListOperation(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"selector", "name"}); // Validation

        List<WebElement> webElements = findElements(script.get("selector").toString(), script.get("name").toString());
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

        LOG.info("Storing capture list as: " + script.get("variable").toString());
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
     * Click on a web element.
     * @param script the click subscript operation
     */
    private void clickOperation(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"selector", "name"}); // Validation

        JavascriptExecutor js = (JavascriptExecutor) driver;
        String selector = script.get("selector").toString();
        String name = script.get("name").toString();

        if(name.contains("{variable}")) {
            name = name.replace("{variable}", this.loopValue.toString());
        }

        WebElement element = driver.findElement(ByElement(selector, name));
        if(element == null) { // If the element wasn't found, pass that info back
            throw new NoSuchElementException("Attempted to click element with a " + selector + " of `" + name + "` but no such element was found!");
        }

        LOG.info("Clicking element with " + selector + " of `" + name + "`");
        element.click();
    }

    /**
     * Click on an item in a list.
     * @param script the click-list-item subscript operation
     */
    private void clickListItemOperation(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"selector", "name"}); // Validation

        List<WebElement> element = findElements(script.get("selector").toString(), script.get("name").toString());
        int i = ((Double) script.get("item")).intValue();
        LOG.info("Clicking list item");
        element.get(i).click();
    }

    /**
     * Process a logical `if` block.
     * @param script if-block subscript operation
     * @throws ParseException occurs when one of the required fields is missing
     * @throws IOException occurs when a snapshot in a child-instruction fails to write to disk
     * @throws InterruptedException occurs when the process wakes up from a sleep event in a child-instruction
     */
    private void ifOperation(Map<String, Object> script) throws ParseException,
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
        LOG.info("Processing `condition` block with " + conditionBody.size() + " instructions: " + condition);

        // If the condition passes, i.e. no TimeoutException's or NoSuchElementException's are thrown during
        //      execution, then run the `then` block, otherwise run the `else` block
        if (runScript(condition)) {
            LOG.info("Condition block succeeded!");
            for(Map<String, String> thenSubBlock: thenBody){
                Map<String, Object> thenBlock = new HashMap<>();
                thenBlock.put("else", thenSubBlock);

                runScript(thenBlock);
            }
        } else if (elseBody != null) {
            LOG.info("Condition block failed!");
            for(Map<String, String> elseSubBlock: elseBody){
                Map<String, Object> elseBlock = new HashMap<>();
                elseBlock.put("else", elseSubBlock);

                runScript(elseBlock);
            }
        }
        else {
            LOG.warn("Condition did not meet, and no `else` clause was specified! Falling through...");
        }
    }

    /**
     * Inject content onto the snapshot stack.
     *      If unspecified, the content is an error message indicating token info was not found.
     * @param script the inject-content subscript instruction
     * @throws ParseException occurs when the required fields are not specified
     */
    private void injectContentOperation(Map<String, Object> script) throws ParseException {
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

        LOG.warn("Injecting " + type + " content onto snapshot stack: `" + content + "`");
        snapshots.add(content);
    }

    /**
     * Go back to the previous page using JS.
     */
    private void jsBackOperation() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            LOG.info("Going to last page");

            //Calling executeAsyncScript() method to go back a page
            js.executeScript("window.history.back();");

            //waits for page to load
            js.executeAsyncScript("window.setTimeout(arguments[arguments.length - 1], 10000);");
            LOG.info("Page refreshed");
        } catch (org.openqa.selenium.NoSuchElementException e) {
            LOG.error("Back operation failed!");
        }
    }

    /**
     * Click on a web element using JS.
     * @param script the js-click subscript operation
     */
    private void jsClickOperation(Map<String, Object> script) throws ParseException, NoSuchElementException {
        validate(script, new String[] {"selector", "name"}); // Validation

        String selector = script.get("selector").toString();
        String name = script.get("name").toString();

        if(name.contains("{variable}")) {
            name = name.replace("{variable}", this.loopValue.toString());
        }

        WebElement element = driver.findElement(ByElement(selector, name));
        if(element == null) { // If the element wasn't found, pass that info back
            throw new NoSuchElementException("Attempted to click element with a " + selector + " of `" + name + "` but no such element was found!");
        }

        LOG.info("JS-clicking element with " + selector + " of `" + name + "`");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    /**
     * Refresh the current page using JS.
     */
    private void jsRefreshOperation() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            LOG.info("Refreshing the page");

            //Calling executeAsyncScript() method to go back a page
            js.executeScript("location.reload();");

            //waits for page to load
            js.executeAsyncScript("window.setTimeout(arguments[arguments.length - 1], 10000);");
        } catch (NoSuchElementException e) {
            LOG.info("Refresh failed");
        }
    }

    /**
     * Send keyboard input to specified web element.
     * @param script the send-key subscript operation
     * @throws InterruptedException occurs when  an interruption signal is raised after sleeping
     * @throws ParseException occurs when the required fields are not specified
     */
    private void keysOperation(Map<String, Object> script) throws InterruptedException, ParseException {
        validate(script, new String[] {"selector", "name", "value"}); // Validation

        WebElement element = findElement(script.get("selector").toString(), script.get("name").toString());
        String input = script.get("value").toString().toLowerCase();
        int charDelay = Integer.parseInt(script.getOrDefault("delay", 300).toString());
        int postInputDelay = Integer.parseInt(script.getOrDefault("postDelay", 5000).toString());

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
                LOG.info("Inserting: " + s);
                element.sendKeys(String.valueOf(s));
                Thread.sleep(charDelay);
            }
            Thread.sleep(postInputDelay); // Wait even more for some reason?
        }
    }

    /**
     * Wait for the web page ready-state to change to `complete`.
     * @param script the load-page subscript operation
     * @throws ParseException occurs if an invalid timeout value was specified
     */
    private void loadPageOperation(Map<String, Object> script) throws ParseException {
        // Fetch or fill the default timeout value
        long timeout = parseNumber(script.getOrDefault("timeout", defaultWaitTimeout).toString()).longValue();

        // Wait for page-state
        LOG.info("Waiting for page to fully load within " + timeout + " seconds: " + driver.getCurrentUrl());
        new WebDriverWait(driver, timeout)
                .until((driver) -> ((JavascriptExecutor) driver).executeScript("return document.readyState")
                        .toString()
                        .equals("complete"));
    }

    /**
     * Loop over a variable and run a subscript on each iteration.
     * @param script the loop subscript operation
     */
    private void loopOperation(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"variable", "subscript"}); // Validation

        String variableName = script.get("variable").toString();
        List<String> vars = captureLists.get(variableName);

        LOG.info("Performing Variable Loop for: " + variableName);
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

    /**
     * Restores the browser to the original URL.
     * @param script the restore subscript operation
     */
    private void restoreOperation(Map<String, Object> script) {
        String url = script.getOrDefault("url", this.url).toString();
        driver.get(url);
    }

    /**
     * Take a screenshot (rasterize image) of the current page.
     * @param script the screenshot subscript operation
     * @throws IOException when a screenshot image fails to write to disk
     */
    public void screenshotOperation(Map<String, Object> script) throws IOException, ParseException {
        validate(script, "targetdir"); // Validation

        TakesScreenshot scrShot = ((TakesScreenshot) driver);

        File f = scrShot.getScreenshotAs(OutputType.FILE);
        File dest = new File((String) script.get("targetdir"));
        FileUtils.copyFile(f, dest);
    }

    /**
     * Interact with a select-dropdown web element.
     * @param script the select subscript operation
     * @throws ParseException occurs when the required fields are not specified
     */
    private void selectOperation(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"selector", "name", "selectBy", "value"}); // Validation

        String selector = script.get("selector").toString();
        String name = script.get("name").toString();
        String selectBy = script.get("selectBy").toString();
        String value = script.get("value").toString();

        Select selectElement = new Select(driver.findElement(ByElement(selector, name)));

        LOG.info("Selecting option in dropdown by `" + selectBy + "`...");
        switch(selectBy.toLowerCase()) {
            case "value":
                selectElement.selectByValue(value);
                break;
            case "index":
                selectElement.selectByIndex(Integer.parseInt(value));
                break;
            case "visible":
                LOG.warn("The selectBy option `visible` is deprecated in favor of `visible-text`.");
            case "visible-text":
                selectElement.selectByVisibleText(value);
                break;
            default:
                throw new ParseException("Invalid `selectBy` option: " + selectBy, 0);
        }
    }

    /**
     * Take a "snapshot" of the current page HTML and store it on the snapshots stack.
     */
    private void snapshotOperation() {
        LOG.info("Taking snapshot of page: " + driver.getCurrentUrl());
        snapshots.add(driver.getPageSource());
    }

    /**
     * Return the snapshots stack.
     * @return List<String> the list of paths to snapshot images taken
     */
    public final List<String> getSnapshots(){
        return snapshots;
    }

    /**
     * Iterate through a tables rows and perform a subscript on each row.
     * @param script the iterate-table subscript operation
     * @throws IOException when a snapshot image failed to save to disk
     * @throws ParseException when a parsing error was found in the script
     * @throws InterruptedException when the process wakes up from a sleep event
     */
    private void tableOperation(Map<String, Object> script) throws IOException, ParseException, InterruptedException {
        // validate(script, new String[] {""}); // Validation

        int offset = Integer.parseInt(script.getOrDefault("rowoffset", 0).toString());

        while (true) {
            List<WebElement>  allRows = findElements(script.get("selector").toString(), script.get("name").toString());
            int tableSize = allRows.size();

            LOG.debug("Found " + tableSize + " rows in table!");

            if(tableSize <= offset){
                break;
            }
            for (int i = offset; i < tableSize; i++) {
                String xpath = script.get("name").toString();
                xpath = xpath + "[" + i + "]";
                allRows = findElements(script.get("selector").toString(), xpath);
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
                    findElement(nextbuttonAttrs.get("selector").toString(), nextbuttonAttrs.get("name").toString());
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
     * Wait for an element to exist and become visible in the browser viewport.
     * @param script the wait subscript operation
     * @throws ParseException occurs when the required fields are not specified
     */
    private void waitOperation(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"selector", "name"}); // Validation

        // Fetch or fill the default timeout value
        long timeout = parseNumber(script.getOrDefault("timeout", defaultWaitTimeout).toString()).longValue();

        // Subscript parameters
        String selector = script.get("selector").toString();
        String name = script.get("name").toString();

        // Inject variable value if keyword is used
        if(name.contains("{variable}")) {
            name = name.replace("{variable}", loopValue.toString());
        }

        LOG.info("Waiting for element with " + selector +  " of `" + name + "` to appear within " + timeout + " seconds...");

        // Wait for element
        WebElement element = new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.visibilityOfElementLocated(ByElement(selector, name)));
        assert element.isDisplayed();
    }
}
