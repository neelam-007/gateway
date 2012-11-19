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
    private Boolean optionalDefault = false; // whether a non-required group should be prompted-for by default
    private Boolean deletable = true; // is this group deletable (if not required)
    private String group; // The group for this option group
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

    @XmlAttribute
    public Boolean isOptionalDefault() {
        return optionalDefault;
    }

    public void setOptionalDefault(Boolean optionalDefault) {
        this.optionalDefault = optionalDefault;
    }

    @XmlAttribute
    public Boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    @XmlAttribute
    public String getGroup() {
        return group;
    }

    public void setGroup( final String group ) {
        this.group = group;
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
