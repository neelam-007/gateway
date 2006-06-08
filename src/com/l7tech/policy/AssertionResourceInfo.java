/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.policy.assertion.AssertionResourceType;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * @author alex
 */
public abstract class AssertionResourceInfo implements Cloneable {
    protected String[] urlRegexes = new String[0];

    public abstract AssertionResourceType getType();

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * @return an array of regular expressions, any of which must match a URL for it to be considered acceptable.
     */
    public String[] getUrlRegexes() {
        return urlRegexes;
    }

    /**
     * @return the regular expressions compiled into patterns.  May be empty but never null.
     * @see #getUrlRegexes()
     */
    public Pattern[] makeUrlPatterns() {
        List patterns = new ArrayList();
        for (int i = 0; i < urlRegexes.length; i++) {
            String regex = urlRegexes[i];
            Pattern p;
            p = Pattern.compile(regex);
            patterns.add(p);
        }
        return (Pattern[])patterns.toArray(new Pattern[0]);
    }

    /**
     * @param urlRegexes an array of regular expressions, any of which must match a URL for it to be considered acceptable.
     */
    public void setUrlRegexes(String[] urlRegexes) {
        if (urlRegexes == null) throw new NullPointerException();
        this.urlRegexes = urlRegexes;
    }
}
