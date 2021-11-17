package uk.co.spicule.seleniumscripter;

import org.openqa.selenium.Proxy;

public class ProxySelector {

    final Proxy proxyObj = new Proxy();
    private String proxyEndpoint;

    public ProxySelector(String endpoint) {
        this.proxyEndpoint = endpoint;
    }


    public Proxy getProxy() {

        System.out.println("Proxy endpoint provided: " + proxyEndpoint);

        proxyObj.setHttpProxy(proxyEndpoint);
        proxyObj.setSslProxy(proxyEndpoint);

        return proxyObj;
    }
}
