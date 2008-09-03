package com.l7tech.server.config.beans;

import com.l7tech.server.config.OSSpecificFunctions;

import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 * Date: Aug 11, 2005
 * Time: 11:38:38 AM
 */
public abstract class BaseConfigurationBean implements ConfigurationBean {

    protected String elementName;
    protected String elementDescription;
    
    protected List<String> explanations;

    protected String insertTab = "\t";
    protected static String eol = System.getProperty("line.separator");

    protected BaseConfigurationBean() {
    }

    public BaseConfigurationBean(String name, String description) {
        elementName = name;
        elementDescription = description;
        explanations = new ArrayList<String>();
    }

    public abstract void reset();

    public String getName() {
        return elementName;
    }

    public String getDescription() {
        return elementDescription;
    }

    public String[] explain() {
        explanations.clear();
        populateExplanations();
        return explanations.toArray(new String[explanations.size()]);
    }

    protected abstract void populateExplanations();
}
