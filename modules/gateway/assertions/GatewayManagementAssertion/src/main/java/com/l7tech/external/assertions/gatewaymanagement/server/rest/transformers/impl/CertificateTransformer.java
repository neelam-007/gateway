package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.CertificateResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.security.cert.TrustedCert;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.security.cert.X509Certificate;

@Component
public class CertificateTransformer extends APIResourceWsmanBaseTransformer<TrustedCertificateMO, TrustedCert,EntityHeader, CertificateResourceFactory> {

    @Override
    @Inject
    protected void setFactory(CertificateResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<TrustedCertificateMO> convertToItem(TrustedCertificateMO m) {
        return new ItemBuilder<TrustedCertificateMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    public CertificateData getCertData(X509Certificate cert){
        try {
            return ManagedObjectFactory.createCertificateData(cert);
        } catch (ManagedObjectFactory.FactoryException e) {
            throw new ResourceFactory.ResourceAccessException(e);
        }
    }

}
