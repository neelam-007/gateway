/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.Assertion;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * An assertion that sends the current request's credentials to a WS-Trust token service and replaces them with
 * the new credentials received from it.
 */
public class WsTrustCredentialExchange extends Assertion {
    public WsTrustCredentialExchange() {
    }

    public WsTrustCredentialExchange(String tokenServiceUrl, String appliesTo, TokenServiceRequestType requestType) {
        this.tokenServiceUrl = tokenServiceUrl;
        this.appliesTo = appliesTo;
        this.requestType = requestType;
    }

    public String getTokenServiceUrl() {
        return tokenServiceUrl;
    }

    public void setTokenServiceUrl(String tokenServiceUrl) {
        this.tokenServiceUrl = tokenServiceUrl;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public TokenServiceRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(TokenServiceRequestType requestType) {
        this.requestType = requestType;
    }

    private String tokenServiceUrl;
    private String appliesTo;
    private TokenServiceRequestType requestType;

    public static final class TokenServiceRequestType implements Serializable {
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

        protected Object readResolve() {
            return fromString(uri);
        }

        public String getUri() { return uri; }
    }
}
