/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.security.xml;

public final class TokenServiceRequestType {
    public static final TokenServiceRequestType ISSUE = new TokenServiceRequestType("http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue");
    public static final TokenServiceRequestType VALIDATE = new TokenServiceRequestType("http://schemas.xmlsoap.org/ws/2004/04/security/trust/Validate");

    private final String uri;
    private TokenServiceRequestType(String uri) { this.uri = uri; }
    String getUri() { return uri; }
}
