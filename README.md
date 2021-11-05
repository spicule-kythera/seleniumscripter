# Selenium Scripter

An expressive script-style method of interacting with the Selenium Framework.

## Local Setup

The test harness uses the [Gekco Driver](https://github.com/mozilla/geckodriver/releases) by default, (an extension of the Firefox web browser).

Ensure you have _Gecko 0.26 or newer_, before using the SeleniumScript test harness.

#### Install Locally
`$` `mvn clean install`

#### Verify Build

`$` `mvn --batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true -DdeployAtEnd=true verify -DskipTests -s ci_settings.xml`

#### Run Tests

`$` `mvn clean test -Dwebdriver.gecko.driver=./geckodriver`

#### Run A Specific Test

`$` `mvn clean test -Dtest=TestSeleniumScripter#testAlabama -Dwebdriver.gecko.driver=./geckodriver`

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/juanresendiz813/seleniumscripter.git)
