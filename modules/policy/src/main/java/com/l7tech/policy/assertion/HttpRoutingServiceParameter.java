package com.l7tech.policy.assertion;

import java.io.Serializable;

/**
 * <p>A bean to hold the header & query paramters.</p>
 *
 * @author rraquepo
 */
public class HttpRoutingServiceParameter implements Serializable {
    public static final String HEADER = "Header";
    public static final String QUERY = "Query";

    private String name;
    private String value;
    private String type;

    public HttpRoutingServiceParameter() {

    };

    public HttpRoutingServiceParameter(final String name, final String value, final String type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
