package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.EmailListenerAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.EmailListenerTransformer;
import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The email listener resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + EmailListenerResource.emailListener_URI)
@Singleton
public class EmailListenerResource extends RestEntityResource<EmailListenerMO, EmailListenerAPIResourceFactory, EmailListenerTransformer> {

    protected static final String emailListener_URI = "emailListeners";

    @Override
    @SpringBean
    public void setFactory(EmailListenerAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(EmailListenerTransformer transformer) {
        super.transformer = transformer;
    }
}
