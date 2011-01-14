package com.l7tech.server.config.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 * Date: Aug 11, 2005
 * Time: 11:38:38 AM
 */
public abstract class BaseConfigurationBean implements ConfigurationBean {

    // - PUBLIC

    public static final String TAB = "\t";
    public static final String EOL = System.getProperty("line.separator");

    public BaseConfigurationBean(String name, String description) {
        elementName = name;
        elementDescription = description;
        explanations = new ArrayList<String>();
    }

    public abstract void reset();

    @Override
    public String getName() {
        return elementName;
    }

    @Override
    public String getDescription() {
        return elementDescription;
    }

    @Override
    public String[] explain() {
        explanations.clear();
        populateExplanations();
        return explanations.toArray(new String[explanations.size()]);
    }

    // - PROTECTED

    protected String elementName;
    protected String elementDescription;
    protected List<String> explanations;

    protected static String concatConfigLines(String separatorChars, List<String> configLines) {
        StringBuilder result = new StringBuilder();
        for (String line : configLines) {
            if ( result.length() >0 ) result.append(separatorChars); // don't start the file with the separator
            result.append(line);
        }
        return result.toString();
    }

    protected abstract void populateExplanations();
}
