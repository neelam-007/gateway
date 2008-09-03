package com.l7tech.gateway.config.client.options;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * An OptionSet represents all options a configuration.
 * 
 * <p>OptionSet can have a parent, in which case it will inherit all of the
 * parents options. Options are overridden if the OptionSet contains an option
 * of the same name as the parent.</p>
 * 
 * @author steve
 */
@XmlRootElement
public class OptionSet {
    private String id;
    private String parentName;
    private String description;
    private String prompt;
    private Set<OptionGroup> groups;
    private Set<Option> options;
    
    public OptionSet() {
        groups = new HashSet<OptionGroup>();
        options = new TreeSet<Option>();
    }

    @XmlAttribute
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute
    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    @XmlElement
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Set<Option> getOptions(String optionSet) {
        Option option = null;
        
        for ( Option currentOption : options ) {
            if ( optionSet.equals(currentOption.getId()) ) {
                option = currentOption;
                break;
            }
        }
        
        if ( option == null ) {
            return Collections.emptySet();
        } else {
            return Collections.singleton(option);
        }
    }

    @XmlElement(name="optionGroup")
    public Set<OptionGroup> getOptionGroups() {
        return groups;
    }

    public void setOptionGroups(Set<OptionGroup> groups) {
        this.groups = new HashSet<OptionGroup>(groups);
    }

    @XmlElement(name="option")
    public Set<Option> getOptions() {
        return options;
    }

    public void setOptions(Set<Option> options) {
        this.options = new TreeSet<Option>(options);
    }   
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OptionSet[");
        builder.append("id=");
        builder.append(id);
        builder.append("; description=");
        builder.append(description);
        builder.append("; options=");
        builder.append(options);
        builder.append(";]");
        return builder.toString();
    }
    
}
