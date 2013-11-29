package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.CertificateRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.TrustedCertificateMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The trusted certificate resource
 */
@Provider
@Path(CertificateResource.trustedCertificate_URI)
public class CertificateResource extends RestEntityResource<TrustedCertificateMO, CertificateRestResourceFactory> {

    protected static final String trustedCertificate_URI = "trustedCertificates";

    @Override
    @SpringBean
    public void setFactory(CertificateRestResourceFactory factory) {
        super.factory = factory;
    }

}
