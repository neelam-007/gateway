package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.CertificateRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.TrustedCertificateMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The trusted certificate resource
 */
@Provider
@Path(CertificateResource.trustedCertificate_URI)
@Singleton
public class CertificateResource extends RestEntityResource<TrustedCertificateMO, CertificateRestResourceFactory> {

    protected static final String trustedCertificate_URI = "trustedCertificates";

    @Override
    @SpringBean
    public void setFactory(CertificateRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Reference<TrustedCertificateMO> toReference(TrustedCertificateMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
