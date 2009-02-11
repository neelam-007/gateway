/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Map;

@XmlRootElement(name="header")
public class Header implements Serializable, Map.Entry<String, String> {
    private String name;
    private String value;

    public Header(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Deprecated
    Header() { }

    @XmlAttribute
    public String getName() {
        return name;
    }

    @Deprecated
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getKey() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Deprecated
    public String setValue(String value) {
        String old = this.value;
        this.value = value;
        return old;
    }
}
