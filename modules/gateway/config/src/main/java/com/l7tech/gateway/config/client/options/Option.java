package com.l7tech.gateway.config.client.options;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * An option is a single configuration item.
 *
 * @author steve
 */
public class Option implements Comparable {
    private String id; // unique id
    private OptionType type; // datatype
    private int order; // order when in wizard mode
    private String configName; // external property name
    private String configValue; // default property value
    private String name; // user visible property name
    private String group; // The group for this option
    private String prompt; // wizard step prompt
    private String description; // property description
    private String regex; // property validation if not default for type
    private boolean updatable = true; // is this property editable
    private boolean confirmed; // does this property require confirmation
    
    @XmlElement
    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    @XmlElement
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlAttribute
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @XmlAttribute
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @XmlAttribute
    public OptionType getType() {
        return type;
    }

    public void setType(OptionType type) {
        this.type = type;
    }

    @XmlElement
    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    @XmlElement
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @XmlElement
    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    @XmlAttribute
    public boolean isUpdatable() {
        return updatable;
    }

    public void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    @XmlAttribute
    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }        
    
    public int compareTo(Object o) {
        int value = Integer.valueOf(order).compareTo( ((Option) o).order );
        if ( value==0 ) {
            value = name.compareTo(((Option) o).name);
        }        
        return value;
    }        
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Option[");
        builder.append("id=");
        builder.append(id);
        builder.append("; type=");
        builder.append(type);
        builder.append("; name=");
        builder.append(name);
        builder.append("; description=");
        builder.append(description);
        builder.append("; confirmed=");
        builder.append(confirmed);
        builder.append("; updatable=");
        builder.append(updatable);
        builder.append(";]");
        return builder.toString();
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Option other = (Option) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }  
}
