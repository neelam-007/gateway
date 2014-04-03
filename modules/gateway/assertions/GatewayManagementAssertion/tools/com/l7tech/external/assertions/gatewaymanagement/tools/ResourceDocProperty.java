package com.l7tech.external.assertions.gatewaymanagement.tools;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This is added to a Resource doc any element list. It can keep track of other properties that we might want to track
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "resourceDocProperty", propOrder = {})
@XmlRootElement(name = "resourceDocProperty")
public class ResourceDocProperty {

    private String name;
    private String value;

    public ResourceDocProperty() {
    }

    public ResourceDocProperty(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

}