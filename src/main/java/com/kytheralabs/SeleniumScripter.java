package com.kytheralabs;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.management.AttributeNotFoundException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Selenium Scripter, generate selenium scripts from YAML.
 */
public class SeleniumScripter {
    // Constant things
    private final String url; // The initial url the agent starts at
    private final WebDriver driver; // The web driver
    private final long defaultWaitTimeout = 30; // The default element wait timeout in seconds
    private final List<String> snapshots = new ArrayList<>(); // The stack of HTML content to return to the crawl
    private final Map<String, List> captureLists = new HashMap<>(); // A list of HTML content for capturing search suggestions
    private final Map<String, Object> scriptVariables = new HashMap<>(); // Variables instantiated by the script

    // Static things
    private static final Logger LOG = LogManager.getLogger(SeleniumScripter.class); // Application logger

    public SeleniumScripter(WebDriver driver) {
        this.driver = driver;
        url = driver.getCurrentUrl();
    }

    /**
     * Slice an array into a sublist
     * @param slice the slice string which must conform to the pattern `^-{0,1}[0-9]+:-{0,1}[0-9]+$`
     * @param list the list to slice
     * @return List<Object> the sliced list
     * @throws ParseException occurs when an invalid slice string is specified
     */
    private List slice(String slice, List list) throws ParseException {
        // Validate that the slice is formatted correctly
        if(!slice.matches("-{0,1}[0-9]+:-{0,1}[0-9]+")) {
            throw new ParseException("Invalid slice specification, must match pattern: `^-{0,1}[0-9]+:-{0,1}[0-9]+$`!", 0);
        }

        String[] parts = slice.split(":");

        // Fetch the raw values
        int start = Integer.parseInt(parts[0]);
        int end = Integer.parseInt(parts[1]);

        // Invert the index position if the number is negative
        if(start < 0) {
            start += list.size();
        }
        if(end < 0) {
            end += list.size();
        }

        return list.subList(start, end);
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
     * Convert selector and value string to a `selenium.By` object
     * @param selector the HTML selection method
     * @param name the value of the selection attribute
     * @throws ParseException occurs when an invalid selector value is specified
     * @return By the desired element
     */
    private By by(String selector, String name) throws ParseException {
        switch (selector) {
            case "id":
                return By.id(name);
            case "class":
                return By.className(name);
            case "css":
            case "cssSelector":
                return By.cssSelector(name);
            case "xpath":
                return By.xpath(name);
            case "name":
                return By.name(name);
            default:
                throw new ParseException("Invalid selector type: `" + selector + "`", 0);
        }
    }

    /**
     * Resolve the value of a variable-expression, if any was specified
     * @param expression the original expression
     * @return the fully resolved expression
     * @throws ValueException occurs when the requested variable name was not instantiated
     */
    private String resolveExpressionValue(String expression) throws ValueException {
        // If the brackets indicators `{}` are not in the expression, then just return the literal value
        if (!expression.matches(".*\\{[a-zA-Z][a-zA-Z_0-9]*}.*")) {
            return expression;
        }

        int start = expression.indexOf("{") + 1;
        int end = expression.indexOf("}");

        // Trim the brackets `{}` to get just the variable name
        String name = expression.subSequence(start, end).toString();

        // Throw a value error if the variable wasn't defined
        if(!scriptVariables.containsKey(name)) {
            throw new ValueException("The script did not instantiate the variable `" + name + "`!");
        }

        // Get the value of the variable
        String value = scriptVariables.get(name).toString();

        return expression.replace("{" + name + "}", value);
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
     * @param script the serialized selenium script
     * @throws IOException occurs when a snapshot image failed to save to disk
     * @throws AttributeNotFoundException occurs when an attribute on a selected element does not exist
     * @throws ParseException occurs when one or more required fields are missing or an invalid value is specified
     * @throws InterruptedException occurs when the process wakes up from a sleep event in a child-instruction
     */
    public void runScript(Map<String, Object> script) throws IOException,
                                                                               AttributeNotFoundException,
                                                                               ParseException,
                                                                               InterruptedException {
        LOG.info("Processing Selenium Script with " + script.size() + " objects!");

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
                    case "for":
                        forOperation(subscript);
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
                    case "try":
                        tryOperation(subscript);
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

        LOG.info(snapshots.size() + " snapshots taken at the end of this block!");
    }

    /**
     * Runs a sequence of instructions
     * @param sequence
     */
    private void runSubsequence(List<Map<String, String>> sequence) throws IOException,
                                                                           AttributeNotFoundException,
                                                                           ParseException,
                                                                           InterruptedException {
        for (Map<String, String> instruction : sequence) {
            Map<String, Object> instructionBlock = new HashMap<>();
            instructionBlock.put("subsequence", instruction);
            runScript(instructionBlock);
        }
    }

    /**
     * Create a capture list.
     *      A "captured list" is a list of elements or labels which can be iterated over, elsewhere.
     * @param script the capture-list subscript operation
     * @throws ParseException occurs when one or more required fields are missing or an invalid value is specified
     */
    private void captureListOperation(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"selector", "name"}); // Validation

        String selector = script.get("selector").toString();
        String name = script.get("name").toString();

        List<WebElement> webElements = driver.findElements(by(selector, name));
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

        // Get the instruction parameters
        String selector = script.get("selector").toString();
        String name = script.get("name").toString();

        // Substitute any specified script-variable-values
        name = resolveExpressionValue(name);

        // Get the element to-be-clicked
        WebElement element = driver.findElement(by(selector, name));

        driver.navigate();

        // Scroll the element into view
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", element);

        // Click-n-go
        LOG.info("Clicking element with " + selector + " of `" + name + "`");
        element.click();
    }

    /**
     * Click on an item in a list.
     * @param script the click-list-item subscript operation
     * @throws ParseException occurs when one or more required fields are missing or an invalid value is specified
     */
    private void clickListItemOperation(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"selector", "name"}); // Validation

        // Get the instruction parameters
        String selector = script.get("selector").toString();
        String name = script.get("name").toString();

        // Substitute any specified script-variable-values
        name = resolveExpressionValue(name);

        List<WebElement> element = driver.findElements(by(selector, name));
        int i = ((Double) script.get("item")).intValue();
        LOG.info("Clicking list item");
        element.get(i).click();
    }

    /**
     * Iterate over a list of items.
     * @param script the click-list-item subscript operation
     * @throws ParseException occurs when one or more required fields are missing or an invalid value is specified
     */
    private void forOperation(Map<String, Object> script) throws ParseException,
                                                                 AttributeNotFoundException,
                                                                 IOException,
                                                                 InterruptedException {
        // Validation
        validate(script, new String[] {"forEach", "do"});
        Map<String, Object> forEachParams = (Map<String, Object>) script.get("forEach");
        validate(forEachParams, new String[] {"selector", "name"});

        // Script parameters
        String selector = forEachParams.get("selector").toString();
        String name = forEachParams.get("name").toString();
        String iteratorName = forEachParams.get("variable").toString();
        List<Map<String, String>> doBlock = (List<Map<String, String>>) script.get("do");

        // Substitute any specified script-variable-values
        name = resolveExpressionValue(name);

        // Fetch Element XPaths to iterate on
        List<WebElement> elements = driver.findElements(by(selector, name));
        List<String> xpaths = new ArrayList<>();
        for(WebElement e : elements) {
            xpaths.add(getElementXPath(e));
        }

        // Slice the list of elements if specified
        if(forEachParams.containsKey("slice")) {
            String slice = forEachParams.get("slice").toString();
            xpaths = slice(slice, xpaths);
        }

        // Iterate
        for (String xpath : xpaths) {
            scriptVariables.put(iteratorName, xpath);
            runSubsequence(doBlock);
        }
    }

    /**
     * Process a logical `if` block.
     * @param script if-block subscript operation
     * @throws ParseException occurs when one or more required fields are missing or an invalid value is specified
     * @throws AttributeNotFoundException occurs when an attribute on a selected element does not exist
     * @throws IOException occurs when a snapshot in a child-instruction fails to write to disk
     * @throws InterruptedException occurs when the process wakes up from a sleep event in a child-instruction
     */
    private void ifOperation(Map<String, Object> script) throws ParseException,
                                                                AttributeNotFoundException,
                                                                IOException,
                                                                InterruptedException {
        validate(script, new String[] {"selector", "name", "condition", "then"}); // Validation

        // Fetch element of focus
        String selector = script.get("selector").toString();
        String name = script.get("name").toString();
        WebElement e = new WebDriverWait(driver, 0)
                .until(ExpectedConditions.presenceOfElementLocated(by(selector, name)));

        // Substitute any specified script-variable-values
        name = resolveExpressionValue(name);

        // Fetch the instruction blocks
        List<String> condition = (List<String>) script.get("condition");
        List<Map<String, String>> thenBody = (List<Map<String, String>>) script.get("then");
        List<Map<String, String>> elseBody = (List<Map<String, String>>) script.get("else");

        // Fetch condition details
        String left_operand = e.getAttribute(condition.get(0));
        if(left_operand == null) {
            throw new AttributeNotFoundException("Element with " + selector + " of `" + name + "` does not have the attribute `" + condition.get(0) + "`!");
        }
        left_operand = left_operand.toLowerCase();
        String operator = condition.get(1).toLowerCase();
        String right_operand = condition.get(2).toLowerCase();

        // Run the comparison
        boolean comparison;
        switch (operator) {
            case "equals":
                comparison = left_operand.equals(right_operand);
                break;
            case "contains":
                comparison = left_operand.contains(right_operand);
                break;
            default:
                throw new ParseException("Invalid comparison operator: `" + operator + "`!", 0);
        }

        // Run the resulting logic block
        if(comparison) {
            runSubsequence(thenBody);
        } else if(elseBody != null) {
            runSubsequence(elseBody);
        }
        else {
            LOG.warn("Condition did not meet, and no `else` clause was specified! Falling through...");
        }
    }

    /**
     * Inject content onto the snapshot stack.
     *      If unspecified, the content is an error message indicating token info was not found.
     * @param script the inject-content subscript instruction
     * @throws ParseException occurs when one or more required fields are missing or an invalid value is specified
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

        // Get the instruction parameters
        String selector = script.get("selector").toString();
        String name = script.get("name").toString();

        // Substitute any specified script-variable-values
        name = resolveExpressionValue(name);

        // Fetch the to-be-clicked element
        WebElement element = driver.findElement(by(selector, name));

        // Run the JS to click-n-go
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
     * @throws ParseException occurs when one or more required fields are missing or an invalid value is specified
     */
    private void keysOperation(Map<String, Object> script) throws InterruptedException, ParseException {
        validate(script, new String[] {"selector", "name", "value"}); // Validation

        // Get all of the instruction parameters or field defaults
        String selector = script.get("selector").toString();
        String name = script.get("name").toString();
        String input = script.get("value").toString().toLowerCase();
        int charDelay = Integer.parseInt(script.getOrDefault("delay", 300).toString());
        int postInputDelay = Integer.parseInt(script.getOrDefault("postDelay", 5000).toString());

        // Substitute any specified script-variable-values
        name = resolveExpressionValue(name);

        // Fetch the input field
        WebElement element = driver.findElement(by(selector, name));

        // Determine the correct
        switch (input) {
            case "{enter}":
                element.sendKeys(Keys.ENTER);
                break;
            case "{return}":
                element.sendKeys(Keys.RETURN);
                break;
            case "{down}":
                element.sendKeys(Keys.ARROW_DOWN);
                break;
            default:  // If input is none of the keywords then slow-type the input
                // Substitute any specified script-variable-values
                input = resolveExpressionValue(input);

                // Clear the input field
                element.clear();

                // Slow-type each character
                for (char s : input.toCharArray()) {
                    LOG.info("Inserting: " + s);
                    element.sendKeys(String.valueOf(s));
                    Thread.sleep(charDelay);
                }
                Thread.sleep(postInputDelay); // Wait some more
                break;
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
     * @throws ParseException occurs when one or more required fields are missing or an invalid value is specified
     */
    private void selectOperation(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"selector", "name", "selectBy", "value"}); // Validation

        // Get the instruction parameters
        String selector = script.get("selector").toString();
        String name = script.get("name").toString();
        String selectBy = script.get("selectBy").toString();
        String value = script.get("value").toString();

        // Substitute any specified script-variable-values
        name = resolveExpressionValue(name);

        // Fetch the element to-be-selected and convert it t a serialized Selection Web Element
        Select selectElement = new Select(driver.findElement(by(selector, name)));

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
     * @throws IOException when a snapshot image failed to save to disk\
     * @throws AttributeNotFoundException occurs when an attribute on a selected element does not exist
     * @throws ParseException when a parsing error was found in the script
     * @throws InterruptedException when the process wakes up from a sleep event
     */
    private void tableOperation(Map<String, Object> script) throws IOException, AttributeNotFoundException, ParseException, InterruptedException {
        // validate(script, new String[] {""}); // Validation

        // Get script parameters or fill the defaults
//        String selector = script.get("name").toString();
//        String name = script.get("name").toString();
//        int offset = Integer.parseInt(script.getOrDefault("rowoffset", 0).toString());
//
//        while (true) {
//            List<WebElement>  rows = driver.findElements(by(selector, name));
//            int tableSize = rows.size();
//
//            LOG.debug("Found " + tableSize + " rows in table!");
//
//            if(tableSize <= offset){
//                break;
//            }
//
//            for (int i = offset; i < tableSize; i++) {
//                name = name + "[" + i + "]";
//                rows = driver.findElements(by(selector, name));
//                rows.get(0).click();
//                Map<String, Object> subscripts = (Map<String, Object>) masterScript.get("subscripts");
//                Map<String, Object> subscript = (Map<String, Object>) subscripts.get(script.get("subscript"));
//                runScript(subscript, null);
//                WebDriverWait wait = new WebDriverWait(driver, 180);
//                LOG.debug("Waiting for object: "+script.get("name").toString());
//                wait.until(ExpectedConditions.visibilityOfElementLocated(by(script.get("selector").toString(), script.get("name").toString())));
//            }
//            if (script.containsKey("nextbuttonscript")) {
//                Map<String, Object> subscripts = (Map<String, Object>) masterScript.get("subscripts");
//                Map<String, Object> subscript = (Map<String, Object>) subscripts.get(script.get("nextbuttonscript"));
//                Map<String, Object> nextbuttonAttrs = (Map<String, Object>) script.get("nextbutton");
//                try{
//                    driver.findElements(by(selector, name));
//                    runScript(subscript, null);
//                } catch(org.openqa.selenium.NoSuchElementException e){
//                    LOG.info("Can't find next button, exiting loop");
//                    break;
//                }
//            } else {
//                LOG.debug("Now more rows left to parse");
//                break;
//            }
//        }
    }

    /**
     * Process a logical `try` block.
     * @param script if-block subscript operation
     * @throws ParseException occurs when one or more required fields are missing or an invalid value is specified
     * @throws AttributeNotFoundException occurs when an attribute on a selected element does not exist
     * @throws IOException occurs when a snapshot in a child-instruction fails to write to disk
     * @throws InterruptedException occurs when the process wakes up from a sleep event in a child-instruction
     */
    private void tryOperation(Map<String, Object> script) throws ParseException,
                                                                 AttributeNotFoundException,
                                                                 IOException,
                                                                 InterruptedException {
        validate(script, new String[] {"try", "catch", "expect"}); // Validation

        // Fetch the instruction blocks
        List<Map<String, String>> tryBody = (List<Map<String, String>>) script.get("try");
        List<Map<String, String>> catchBody = (List<Map<String, String>>) script.get("catch");
        List<String> raw_expect = ((List<String>) script.get("expect"))
                                                        .stream()
                                                        .map(String::toLowerCase)
                                                        .collect(Collectors.toList());

        try {
            // Try to run the sequence of instructions
            runSubsequence(tryBody);
        } catch (Exception e) {
            // Fetch the error root name
            String[] parts = e.getClass().toString().split("\\.");
            String name = parts[parts.length - 1].toLowerCase();

            if(raw_expect.contains(name)) { // If the error type was specified, run the catch block
                // Run the catch sequence of instructions if any of the special exceptions occur
                LOG.warn("Caught specified error of type " + e.getClass() + " inside a try operation:");
                e.printStackTrace();

                runSubsequence(catchBody);
            } else { // Otherwise, re-throw the error
                throw e;
            }
        }
    }

    /**
     * Wait for an element to exist and become visible in the browser viewport.
     * @param script the wait subscript operation
     * @throws ParseException occurs when one or more required fields are missing or an invalid value is specified
     */
    private void waitOperation(Map<String, Object> script) throws ParseException {
        validate(script, new String[] {"selector", "name"}); // Validation

        // Fetch or fill the default timeout value
        long timeout = parseNumber(script.getOrDefault("timeout", defaultWaitTimeout).toString()).longValue();

        // Get the instruction parameters
        String selector = script.get("selector").toString();
        String name = script.get("name").toString();

        // Substitute any specified script-variable-values
        name = resolveExpressionValue(name);

        // Wait for element
        LOG.info("Waiting for element with " + selector +  " of `" + name + "` to appear within " + timeout + " seconds...");
        WebElement element = new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.visibilityOfElementLocated(by(selector, name)));
        assert element.isDisplayed();
    }
}
