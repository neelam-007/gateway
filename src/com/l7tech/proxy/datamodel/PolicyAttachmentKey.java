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
 * User: mike
 * Date: Jul 2, 2003
 * Time: 6:29:56 PM
 */
public class PolicyAttachmentKey implements Serializable, Cloneable, Comparable {
    private String uri;
    private String soapAction;
    private String proxyUri;

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
        if (uri != null) code += uri.hashCode();
        if (soapAction != null) code += soapAction.hashCode();
        if (proxyUri != null) code += proxyUri.hashCode();
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
}
