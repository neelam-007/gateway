package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.codehaus.jackson.annotate.JsonProperty;

public class PolicyTemplateArgument {

    private String value;
    private String name;

    @JsonProperty( value = "Value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @JsonProperty( value = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
