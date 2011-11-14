package com.l7tech.server.config.systemconfig;

import java.util.List;

/**
 * User: megery
 */
public abstract class AuthTypeView {
    public abstract List<String> asConfigLines();

    public abstract List<String> describe();

    //convenience method to return a string containing name="value"
    protected String makeNameValuePair(final String name, final String value) {
        String newName = name;
        if (newName == null) {
            newName = "null";
        }
        String newValue = value;
        if (newValue == null) {
            newValue = "null";
        }

        return newName + "=" + "\"" + newValue + "\"";
    }
}
