/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.policy.assertion.AssertionResourceType;

/**
 * A {@link AssertionResourceInfo} implementation describing that the resource should be retrieved from
 * a particular URL.  The URL can include variable references.
 */
public class SingleUrlResourceInfo extends AssertionResourceInfo {
    private String url;

    public SingleUrlResourceInfo(String url) {
        this.url = url;
    }

    public SingleUrlResourceInfo() {
    }

    public AssertionResourceType getType() {
        return AssertionResourceType.SINGLE_URL;
    }

    public String[] getUrlRegexes() {
        // The administrator provided this URL, so we'll trust any URLs he included in it, regardless
        // of what they appear to be pointed at.
        return new String[] { ".*" };
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SingleUrlResourceInfo that = (SingleUrlResourceInfo) o;

        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
    }

    public int hashCode() {
        return (url != null ? url.hashCode() : 0);
    }
}
