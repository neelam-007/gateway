/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;


/**
 * Represents a routing assertion that provides the full functionality of the SecureSpan Bridge.
 */
public class BridgeRoutingAssertion extends HttpRoutingAssertion {
    public BridgeRoutingAssertion(String protectedServiceUrl, String login, String password, String realm, int maxConnections) {
        super(protectedServiceUrl, login, password, realm, maxConnections);
    }

    public BridgeRoutingAssertion() {
        this(null, null, null, null, -1);
    }

    /** @return the hardcoded policy XML for this Bridge instance, or null if policies will be discovered automatically. */
    public String getPolicyXml() {
        return policyXml;
    }

    /** @param policyXml the hardcoded policy for this Bridge instance, or null to discover policies automatically. */
    public void setPolicyXml(String policyXml) {
        this.policyXml = policyXml;
    }

    protected String policyXml = null;
}
