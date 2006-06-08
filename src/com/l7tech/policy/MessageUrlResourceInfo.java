/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.policy.assertion.AssertionResourceType;

import java.util.Arrays;

/**
 * A {@link AssertionResourceInfo} implementation indicating that resources can be fetched from
 * URLs specified within messages, and containing the settings describing how it should be
 * performed.
 */
public class MessageUrlResourceInfo extends AssertionResourceInfo {
    private boolean allowMessagesWithoutUrl = false;

    public MessageUrlResourceInfo() {
    }

    public MessageUrlResourceInfo(String[] urlRegexes) {
        this.urlRegexes = urlRegexes;
    }

    public AssertionResourceType getType() {
        return AssertionResourceType.MESSAGE_URL;
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
