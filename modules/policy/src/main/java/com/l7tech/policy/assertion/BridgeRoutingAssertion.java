/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import java.util.*;

/**
 * Represents a routing assertion that provides the full functionality of the SecureSpan Bridge.
 */
@RequiresSOAP()
public class BridgeRoutingAssertion extends HttpRoutingAssertion implements UsesEntities {

    //- PUBLIC

    public BridgeRoutingAssertion(String protectedServiceUrl, String login, String password, String realm, int maxConnections) {
        super(protectedServiceUrl, login, password, realm, maxConnections);
    }

    public BridgeRoutingAssertion() {
        this(null, null, null, null, -1);
    }

    /** Configure this BRA using the settings from the specified BRA. */
    public void copyFrom( final BridgeRoutingAssertion source ) {
        super.copyFrom(source);
        this.setPolicyXml(source.getPolicyXml());
        this.setServerCertificateOid(source.getServerCertificateOid());
        this.setUseSslByDefault(source.isUseSslByDefault());
        this.setHttpPort(source.getHttpPort());
        this.setHttpsPort(source.getHttpsPort());
        this.setClientPolicyProperties(source.getClientPolicyProperties());
    }

    /** @return the hardcoded policy XML for this Bridge instance, or null if policies will be discovered automatically (only works with SSGs). */
    public String getPolicyXml() {
        return policyXml;
    }

    /** @param policyXml the hardcoded policy for this Bridge instance, or null to discover policies automatically (only wokrs with SSGs). */
    public void setPolicyXml(String policyXml) {
        this.policyXml = policyXml;
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

    public void setServerCertificateName(String name) {
        this.serverCertificateName = name;
    }

    /**
     * Check if SSL should be used when downloading policies, and when sending the initial request when
     * no policy has been configured for a request.
     *
     * @return true if SSL will be used for policy downloads and initial requests that miss the policy cache
     */
    public boolean isUseSslByDefault() {
        return useSslByDefault;
    }

    /**
     * Set whether SSL should be used when downloading policies, and when sending the initial request
     * when no policy has been configured for a request.
     *
     * @param useSslByDefault true if SSL should be used for policy downloads and initial requests that miss the policy cache
     */
    public void setUseSslByDefault(boolean useSslByDefault) {
        this.useSslByDefault = useSslByDefault;
    }

    /**
     * Get the custom HTTP port, if any.
     * If no custom HTTP port is set the BRA will choose a default HTTP port based on the protected service
     * URL.
     *
     * @return the custom HTTP port, or zero to use the default value.
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * Set or clear the custom HTTP port.
     * If no custom HTTP port is set the BRA will choose a default HTTP port based on the protected service
     * URL.
     *
     * @param httpPort the custom HTTP port, or zero to use the default value.
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * Get the custom HTTPS port, if any.
     * If no custom HTTPS port is set the BRA will choose a default HTTPS port based on the protected service
     * URL.
     *
     * @return the custom HTTPS port, or zero to use the default value.
     */
    public int getHttpsPort() {
        return httpsPort;
    }

    /**
     * Set or clear the custom HTTPS port.
     * If no custom HTTPS port is set the BRA will choose a default HTTPS port based on the protected service
     * URL.
     *
     * @param httpsPort the custom HTTPS port, or zero to use the default value.
     */
    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public Map<String, String> getClientPolicyProperties() {
        return clientPolicyProperties;
    }

    public void setClientPolicyProperties( final Map<String, String> properties) {
        this.clientPolicyProperties = properties;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        if (serverCertificateOid != null) {
            return new EntityHeader[] { new EntityHeader(serverCertificateOid.toString(), EntityType.TRUSTED_CERT, serverCertificateName, "Trusted certificate to be used by the bridge routing assertion")};
        } else {
            return new EntityHeader[0];
        }
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(oldEntityHeader.getType().equals(EntityType.TRUSTED_CERT) && serverCertificateOid != null &&
                oldEntityHeader.getOid() == serverCertificateOid && newEntityHeader.getType().equals(EntityType.TRUSTED_CERT))
        {
            serverCertificateOid = newEntityHeader.getOid();
            serverCertificateName = newEntityHeader.getName();
        }
    }

    //- PRIVATE

    private Map<String,String> clientPolicyProperties = new LinkedHashMap<String,String>();
    private String policyXml = null;
    private Long serverCertificateOid = null;
    private String serverCertificateName = null;
    private boolean useSslByDefault = true;
    private int httpPort = 0;
    private int httpsPort = 0;

}
