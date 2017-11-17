package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * This is the mapping object. It is used to describe the mapping used for import and export.
 */
@XmlRootElement(name = "LogSinkFilter")
@XmlType(name = "LogSinkFilterType", propOrder = {"type","values"})
public class LogSinkFilter {
    private String type;
    private List<String> values;

    LogSinkFilter(){}

    LogSinkFilter(LogSinkFilter mapping) {
        this.type = mapping.getType();
        this.values = mapping.getValues();
    }

    @XmlElement(name = "Type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlElement(name = "Values")
    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
