package com.l7tech.external.assertions.icapantivirusscanner;

import java.io.Serializable;

/**
 * <p>A bean to hold the parameters for an Icap request.</p>
 */
public class IcapServiceParameter implements Serializable {

    public static final String HEADER = "Header";
    public static final String QUERY = "Query";

    private String name;
    private String value;
    private String type;

    public IcapServiceParameter(){

    };

    public IcapServiceParameter(final String name, final String value, final String type) {
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
