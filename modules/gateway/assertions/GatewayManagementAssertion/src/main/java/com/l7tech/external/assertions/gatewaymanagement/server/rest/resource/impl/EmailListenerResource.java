package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.EmailListenerRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The email listener resource
 */
@Provider
@Path(EmailListenerResource.emailListener_URI)
@Singleton
public class EmailListenerResource extends RestEntityResource<EmailListenerMO, EmailListenerRestResourceFactory> {

    protected static final String emailListener_URI = "emailListeners";

    @Override
    @SpringBean
    public void setFactory(EmailListenerRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Reference<EmailListenerMO> toReference(EmailListenerMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
