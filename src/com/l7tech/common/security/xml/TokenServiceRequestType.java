/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.security.xml;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class TokenServiceRequestType implements Serializable {
    private static final Map valueMap = new HashMap();
    public static final TokenServiceRequestType ISSUE = new TokenServiceRequestType("Issue", "http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue");
    public static final TokenServiceRequestType VALIDATE = new TokenServiceRequestType("Validate", "http://schemas.xmlsoap.org/ws/2004/04/security/trust/Validate");

    private final String name;
    private final String uri;

    private TokenServiceRequestType(String name, String uri) {
        this.name = name;
        this.uri = uri;
        valueMap.put(uri, this);
    }

    public String toString() {
        return name;
    }

    public static TokenServiceRequestType fromString(String uri) {
        return (TokenServiceRequestType)valueMap.get(uri);
    }

    protected Object readResolve() throws ObjectStreamException {
        return fromString(uri);
    }

    public String getUri() { return uri; }
}
