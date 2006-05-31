/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.policy.assertion.AssertionResourceType;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * A {@link AssertionResourceInfo} implementation indicating that resources can be fetched from
 * URLs specified within messages, and containing the settings describing how it should be
 * performed.
 */
public class MessageUrlResourceInfo extends AssertionResourceInfo {
    private boolean allowMessagesWithoutUrl = false;
    private String[] urlRegexes = new String[0];

    public MessageUrlResourceInfo() {
    }

    public MessageUrlResourceInfo(String[] urlRegexes) {
        this.urlRegexes = urlRegexes;
    }

    public AssertionResourceType getType() {
        return AssertionResourceType.MESSAGE_URL;
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

    /**
     * @return <code>true</code> if messages that do not specify a resource URL should be allowed to pass through
     * without being processed according to the (nonexistent) resource; <code>false</code> otherwise.
     */
    public boolean isAllowMessagesWithoutUrl() {
        return allowMessagesWithoutUrl;
    }

    /**
     * @param allowMessagesWithoutUrl <code>true</code> if messages that do not specify a resource URL
     * should be allowed to pass through without being processed according to the (nonexistent) resource;
     * <code>false</code> otherwise.
     */
    public void setAllowMessagesWithoutUrl(boolean allowMessagesWithoutUrl) {
        this.allowMessagesWithoutUrl = allowMessagesWithoutUrl;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageUrlResourceInfo that = (MessageUrlResourceInfo) o;

        if (allowMessagesWithoutUrl != that.allowMessagesWithoutUrl) return false;
        if (!Arrays.equals(urlRegexes, that.urlRegexes)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (allowMessagesWithoutUrl ? 1 : 0);
        result = 31 * result + (urlRegexes != null ? Arrays.hashCode(urlRegexes) : 0);
        return result;
    }
}
