/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.util.HexUtils;

import java.io.Serializable;

/**
 * Represents a binding between a policy and the kinds of requests it applies to.
 */
public class PolicyAttachmentKey implements Serializable, Cloneable, Comparable {
    private String uri;
    private String soapAction;
    private String proxyUri;
    private boolean beginsWithMatch = false;
    private boolean persistent = false;

    /**
     * No-arg constructor for bean deserializer.
     */
    public PolicyAttachmentKey() {
        uri = "";
        soapAction = "";
        proxyUri = "";
    }

    /**
     * Create a PolicyAttachmentKey using the specified namespace URI, soapAction, and proxy URI local part.
     * @param uri          the namespace URI, or null if there isn't one
     * @param soapAction   the soapAction, or null if there isn't one
     * @param proxyUri     the local part of the original local URL for this request (see URL.getFile())
     */
    public PolicyAttachmentKey(String uri, String soapAction, String proxyUri) {
        this.uri = uri != null ? uri : "";
        this.soapAction = soapAction != null ? soapAction : "";
        this.proxyUri = proxyUri != null ? proxyUri : "";
        this.persistent = false;
        this.beginsWithMatch = false;
    }

    /**
     * Create a copy of the source PolicyAttachmentKey, but without copying persistent and beginsWithMatch.
     * @param source the source to copy.  Must not be null.
     * @throws NullPointerException if it is.
     */
    public PolicyAttachmentKey(PolicyAttachmentKey source) {
        this.uri = source.uri;
        this.soapAction = source.soapAction;
        this.proxyUri = source.proxyUri;
    }

    public int compareTo(Object o) {
        int result;
        PolicyAttachmentKey other = (PolicyAttachmentKey)o;

        result = HexUtils.compareNullable(uri, other.uri);
        if (result != 0)
            return result;

        result = HexUtils.compareNullable(soapAction, other.soapAction);
        if (result != 0)
            return result;

        result = HexUtils.compareNullable(proxyUri, other.proxyUri);
        if (result != 0)
            return result;

        return result;
    }

    public int hashCode() {
        int code = 0;
        if (uri != null) code += 17 * uri.hashCode() + 1;
        if (soapAction != null) code += 79 * soapAction.hashCode() + 7;
        if (proxyUri != null) code += 197 * proxyUri.hashCode() + 13;
        return code;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PolicyAttachmentKey))
            return false;
        return compareTo(obj) == 0;
    }

    // accessors

    /** @return   the namespace URI.  May be empty, but never null. */
    public String getUri() {
        return uri;
    }

    /** @return  the soapAction.  May be empty, but never null.  */
    public String getSoapAction() {
        return soapAction;
    }

    /** @return the local part of the original local URL for this request (see URL.getFile()). May be empty but never null. */
    public String getProxyUri() {
        return proxyUri;
    }

    /** Set the namespace URI. This is here for the xml bean deserializer. */
    public void setUri(String uri) {
        if (uri == null) uri = "";
        this.uri = uri;
    }

    /** Set the SOAPAction. This is here for the xml bean deserializer. */
    public void setSoapAction(String soapAction) {
        if (soapAction == null) soapAction = "";
        this.soapAction = soapAction;
    }

    /** Set the Proxy URI. This is here for the xml bean deserializer. */
    public void setProxyUri(String proxyUri) {
        if (proxyUri == null) proxyUri = "";
        this.proxyUri = proxyUri;
    }

    /**
     * @return true if the non-null fields in this PolicyAttachmentKey should be matched as begins-with matches;
     *         false if they should be matched as exact matches.  Default is false.
     */
    public boolean isBeginsWithMatch() {
        return beginsWithMatch;
    }

    /**
     * @param beginsWithMatch true if the non-null fields in this PolicyAttachmentKey should be matched as begins-with matches;
     *                        false if they should be matched as exact matches.
     */
    public void setBeginsWithMatch(boolean beginsWithMatch) {
        this.beginsWithMatch = beginsWithMatch;
    }

    /**
     * @return  true iff. this key and its associated policy should be saved to disk.
     */
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * @param persistent  true iff. this key and its associated policy should be saved to disk.
     */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }
}
