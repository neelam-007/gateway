/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
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

    /** @return the hardcoded policy XML for this Bridge instance, or null if policies will be discovered automatically (only works with SSGs). */
    public String getPolicyXml() {
        return policyXml;
    }

    /** @param policyXml the hardcoded policy for this Bridge instance, or null to discover policies automatically (only wokrs with SSGs). */
    public void setPolicyXml(String policyXml) {
        this.policyXml = policyXml;
    }

    /** @return the Base64-encoded hardcoded server certificate for this Bridge instance, or null if the server cert will be discovered automatically (only works with SSGs). */
    public String getServerCertBase64() {
        return serverCertBase64;
    }

    /** @param serverCertBase64 the Base64-encoded hardcoded server certificate for this Bridge instance, or null to attempt to use server cert discovery (only works with SSGs). */
    public void setServerCertBase64(String serverCertBase64) {
        this.serverCertBase64 = serverCertBase64;
    }

    /** Configure this BRA using the settings from the specified BRA. */
    public void copyFrom(BridgeRoutingAssertion source) {
        super.copyFrom(source);
        this.setPolicyXml(source.getPolicyXml());

    }

    protected String policyXml = null;
    protected String serverCertBase64 = null;
}
