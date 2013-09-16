/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.util.XmlSafe;

/**
 * {@link AssertionResourceInfo} implementation that specifies that a static document is to be used.
 */
@XmlSafe
public class StaticResourceInfo extends AssertionResourceInfo {
    private String originalUrl;
    private String document;

    @XmlSafe
    public StaticResourceInfo(String document) {
        this.document = document;
    }

    @XmlSafe
    public StaticResourceInfo() {
    }

    @XmlSafe
    public AssertionResourceType getType() {
        return AssertionResourceType.STATIC;
    }

    @XmlSafe
    public String[] getUrlRegexes() {
        // The administrator provided this schema, so we'll trust any URLs he included in it, regardless
        // of what they appear to be pointed at.
        return new String[] { ".*" };
    }

    @XmlSafe
    public String getOriginalUrl() {
        return originalUrl;
    }

    @XmlSafe
    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    @XmlSafe
    public String getDocument() {
        return document;
    }

    @XmlSafe
    public void setDocument(String document) {
        this.document = document;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StaticResourceInfo that = (StaticResourceInfo) o;

        if (document != null ? !document.equals(that.document) : that.document != null) return false;
        if (originalUrl != null ? !originalUrl.equals(that.originalUrl) : that.originalUrl != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (originalUrl != null ? originalUrl.hashCode() : 0);
        result = 31 * result + (document != null ? document.hashCode() : 0);
        return result;
    }
}
