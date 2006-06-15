/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.policy.assertion.AssertionResourceType;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * @author alex
 */
public abstract class AssertionResourceInfo implements Cloneable, Serializable {
    public abstract AssertionResourceType getType();

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * @return an array of regular expressions, any of which must match a URL for it to be considered acceptable.
     */
    public abstract String[] getUrlRegexes();

    /**
     * @return the regular expressions compiled into patterns.  May be empty but never null.
     * @see #getUrlRegexes()
     */
    public Pattern[] makeUrlPatterns() throws PatternSyntaxException {
        List patterns = new ArrayList();
        String[] urlRegexes = getUrlRegexes();
        for (int i = 0; i < urlRegexes.length; i++) {
            String regex = urlRegexes[i];
            Pattern p;
            p = Pattern.compile(regex);
            patterns.add(p);
        }
        return (Pattern[])patterns.toArray(new Pattern[0]);
    }
}
