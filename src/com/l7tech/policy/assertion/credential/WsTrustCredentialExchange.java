/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.common.xml.WsTrustRequestType;

/**
 * An assertion that sends the current request's credentials to a WS-Trust token service and replaces them with
 * the new credentials received from it.
 */
public class WsTrustCredentialExchange extends Assertion {
    public WsTrustCredentialExchange() {
    }

    public WsTrustCredentialExchange(String tokenServiceUrl, String appliesTo, WsTrustRequestType requestType) {
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

    public WsTrustRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(WsTrustRequestType requestType) {
        this.requestType = requestType;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    private String tokenServiceUrl;
    private String appliesTo;
    private String issuer;
    private WsTrustRequestType requestType;

}
