package com.l7tech.external.assertions.portaldeployer.server.client.util;

/**
 * @author rraquepo, 6/17/13
 */
public class BasicNameValuePair implements NameValuePair {

    public BasicNameValuePair(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    private String name;
    private String value;
}
