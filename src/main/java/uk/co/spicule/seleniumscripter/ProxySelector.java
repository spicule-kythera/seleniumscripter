package uk.co.spicule.seleniumscripter;

import org.openqa.selenium.Proxy;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ProxySelector {

    final Proxy proxyObj = new Proxy();

    final List<String> proxyEndpoints = Arrays.asList("us-wa.proxymesh.com:31280",
            "us-il.proxymesh.com:31280",
            "us.proxymesh.com:31280",
            "us-dc.proxymesh.com:31280",
            "us-ca.proxymesh.com:31280",
            "us-ny.proxymesh.com:31280",
            "us-fl.proxymesh.com:31280");

    public Proxy getProxy() {

        int totalEndpoints = proxyEndpoints.size();

        if(totalEndpoints > 0) {

            Random randomObj = new Random();

            int endpointNumber = randomObj.nextInt(totalEndpoints);
            String proxyEndpoint = proxyEndpoints.get(endpointNumber);

            System.out.println("Total endpoints: " + totalEndpoints);
            System.out.println("Selected Proxy Endpoint: " + proxyEndpoint);

            proxyObj.setHttpProxy(proxyEndpoint);
            proxyObj.setSslProxy(proxyEndpoint);

            proxyEndpoints.remove(endpointNumber);
        }

        return proxyObj;
    }
}
