package com.l7tech.gateway.config.client.options;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Group of configuration options.
 *
 * <p>A group of configuration options, e.g. database options</p>
 */
public class OptionGroup {
    private String id; // unique id
    private String prompt; // wizard step prompt
    private String description; // property description

    @XmlAttribute
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @XmlElement
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
