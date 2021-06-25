# Selenium Scripter

A generic yaml script-runner for the Selenium framework. 

## Local Setup

This framework runs on Firefox for local development, so ensure you have a functioning copy of Firefox locally.

Then you need a copy of the Gekco webdriver: https://github.com/mozilla/geckodriver/releases

#### Install Locally
`$` `mvn clean install`

#### Verify Build

`$` `mvn --batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true -DdeployAtEnd=true verify -DskipTests -s ci_settings.xml`

#### Run Tests

`$` `mvn clean test -Dwebdriver.gecko.driver=./geckodriver`

#### Run A Specific Test

`$` `mvn clean test -Dtest=TestSeleniumScripter#testAlabama -Dwebdriver.gecko.driver=./geckodriver`
