/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

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

    public PolicyAttachmentKey() {}

    public PolicyAttachmentKey(String uri, String soapAction) {
        this.uri = uri;
        this.soapAction = soapAction;
    }

    // String compare that treats null as being less than any other string
    private int compareStrings(String s1, String s2) {
        if (s1 == null && s2 == null)
            return 0;
        else if (s1 == null)
            return -1;
        else if (s2 == null)
            return 1;
        else
            return s1.compareTo(s2);
    }

    public int compareTo(Object o) {
        int result;
        PolicyAttachmentKey other = (PolicyAttachmentKey)o;

        result = compareStrings(uri, other.uri);
        if (result != 0)
            return result;

        result = compareStrings(soapAction, other.soapAction);
        if (result != 0)
            return result;

        return result;
    }

    public int hashCode() {
        int code = 0;
        if (uri != null) code += uri.hashCode();
        if (soapAction != null) code += soapAction.hashCode();
        return code;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PolicyAttachmentKey))
            return false;
        return compareTo(obj) == 0;
    }

    // Generated accessors and mutators

    public String getUri() {
        return uri;
    }

    /**
     * @deprecated this is just here for the bean serializer. You should create a new PolicyAttachmentKey instead.
     * @param uri
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getSoapAction() {
        return soapAction;
    }

    /**
     * @deprecated this is just here for the bean serializer. You should create a new PolicyAttachmentKey instead.
     * @param soapAction
     */
    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }
}
