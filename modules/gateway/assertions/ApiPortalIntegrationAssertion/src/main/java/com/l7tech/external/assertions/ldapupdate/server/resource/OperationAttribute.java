package com.l7tech.external.assertions.ldapupdate.server.resource;

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author rraquepo
 */
@XmlRootElement(name = "attribute", namespace = JAXBResourceUnmarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class OperationAttribute {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public OperationAttributeValues getValues() {
        return values;
    }

    public void setValues(OperationAttributeValues values) {
        this.values = values;
    }

    @XmlElement(name = "name", namespace = JAXBResourceUnmarshaller.NAMESPACE, nillable = false)
    private String name = StringUtils.EMPTY;
    @XmlElement(name = "value", namespace = JAXBResourceUnmarshaller.NAMESPACE, nillable = true)
    private String value = StringUtils.EMPTY;
    @XmlElement(name = "action", namespace = JAXBResourceUnmarshaller.NAMESPACE, nillable = true)
    private String action = StringUtils.EMPTY;
    @XmlElement(name = "format", namespace = JAXBResourceUnmarshaller.NAMESPACE, nillable = true)
    private String format = StringUtils.EMPTY;
    @XmlElement(name = "values", namespace = JAXBResourceUnmarshaller.NAMESPACE, nillable = true)
    private OperationAttributeValues values = new OperationAttributeValues();
}
