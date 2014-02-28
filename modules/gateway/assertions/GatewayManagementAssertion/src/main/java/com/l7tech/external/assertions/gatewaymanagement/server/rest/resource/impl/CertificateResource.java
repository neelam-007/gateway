package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.CertificateAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.CertificateTransformer;
import com.l7tech.gateway.api.TrustedCertificateMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The trusted certificate resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + CertificateResource.trustedCertificate_URI)
@Singleton
public class CertificateResource extends RestEntityResource<TrustedCertificateMO, CertificateAPIResourceFactory, CertificateTransformer> {

    protected static final String trustedCertificate_URI = "trustedCertificates";

    @Override
    @SpringBean
    public void setFactory(CertificateAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(CertificateTransformer transformer) {
        super.transformer = transformer;
    }
}
