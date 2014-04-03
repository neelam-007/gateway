package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.CertificateResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.CertificateData;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.TrustedCertificateMO;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.math.BigInteger;

/**
 *
 */
@Component
public class CertificateAPIResourceFactory extends WsmanBaseResourceFactory<TrustedCertificateMO, CertificateResourceFactory> {

    public CertificateAPIResourceFactory() {
    }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.TRUSTED_CERT.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.CertificateResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public TrustedCertificateMO getResourceTemplate() {
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Trusted Certificate Template");
        trustedCertificateMO.setRevocationCheckingPolicyId(new Goid(0,1).toString());
        trustedCertificateMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("trustedForSigningClientCerts", true)
                .put("trustedForSigningServerCerts", true)
                .put("trustedAsSamlAttestingEntity", true)
                .put("trustedAsSamlIssuer", true)
                .put("trustedForSsl", true)
                .put("trustAnchor", true)
                .put("verifyHostname", true)
                .put("revocationCheckingEnabled", true)
                .map());
        CertificateData certificateData = ManagedObjectFactory.createCertificateData();
        certificateData.setEncoded("Encoded Data".getBytes());
        certificateData.setIssuerName("cn=issuerdn");
        certificateData.setSubjectName("cn=subjectdn");
        certificateData.setSerialNumber(new BigInteger("123"));
        trustedCertificateMO.setCertificateData(certificateData);
        return trustedCertificateMO;
    }
}
