package com.l7tech.config.client.options;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Group of configuration options.
 *
 * <p>A group of configuration options, e.g. database options</p>
 */
@XmlType(propOrder={"description","prompt"})
public class OptionGroup {
    private String id; // unique id
    private Boolean required = true; // is this group required
    private String prompt; // wizard step prompt
    private String description; // property description

    @XmlAttribute(required=true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute
    public Boolean isRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        if ( required == null ) throw new NullPointerException(); 
        this.required = required;
    }    
           
    @XmlElement(required=true)
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @XmlElement(required=true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
