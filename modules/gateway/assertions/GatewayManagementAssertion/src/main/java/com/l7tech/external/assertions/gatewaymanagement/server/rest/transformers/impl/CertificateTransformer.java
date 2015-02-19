package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.common.io.CertUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.CertificateResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Component
public class CertificateTransformer extends APIResourceWsmanBaseTransformer<TrustedCertificateMO, TrustedCert,EntityHeader, CertificateResourceFactory> {

    @Override
    @Inject
    protected void setFactory(CertificateResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<TrustedCertificateMO> convertToItem(@NotNull TrustedCertificateMO m) {
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

    @NotNull
    public X509Certificate getX509Certificate(@NotNull final CertificateData certificateData) throws ResourceFactory.InvalidResourceException{
        final Certificate certificate;
        try {
            certificate = CertUtils.getFactory().generateCertificate(
                    new ByteArrayInputStream( certificateData.getEncoded() ) );
        } catch (CertificateException e) {
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid certificate data: " + ExceptionUtils.getMessageWithCause(e));
        }

        if ( !(certificate instanceof X509Certificate) )
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "unexpected encoded certificate type");

        return (X509Certificate) certificate;
    }

}
