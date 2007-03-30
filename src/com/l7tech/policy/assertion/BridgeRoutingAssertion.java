/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.RequiresSOAP;


/**
 * Represents a routing assertion that provides the full functionality of the SecureSpan Bridge.
 */
@RequiresSOAP()
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

    /** Configure this BRA using the settings from the specified BRA. */
    public void copyFrom(BridgeRoutingAssertion source) {
        super.copyFrom(source);
        this.setPolicyXml(source.getPolicyXml());
        this.setServerCertificateOid(source.getServerCertificateOid());
    }

    /**
     * @return the OID of a certificate in the Trusted Certificates table that will be used as the server certificate for
     *          both SSL and message-level crypto, or null if the BRA should attempt to discover the server cert
     *          automatically (by sniffing from an SSL connection, after ensuring server cert is in the Trusted Certs table).
     */
    public Long getServerCertificateOid() {
        return serverCertificateOid;
    }

    /**
     * @param serverCertificateOid the OID of a certificate in the Trusted Certificates table that will be used as
     *                             the server certificate for both SSL and message-level crypto, or null
     *                             to attempt to discover the server cert automatically.
     */
    public void setServerCertificateOid(Long serverCertificateOid) {
        this.serverCertificateOid = serverCertificateOid;
    }

    protected String policyXml = null;
    protected Long serverCertificateOid = null;
}
