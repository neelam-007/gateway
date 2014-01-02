package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.GenericEntityRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The generic entity resource
 */
@Provider
@Path(GenericEntityResource.genericEntity_URI)
@Singleton
public class GenericEntityResource extends RestEntityResource<GenericEntityMO, GenericEntityRestResourceFactory> {

    protected static final String genericEntity_URI = "genericEntities";

    @Override
    @SpringBean
    public void setFactory(GenericEntityRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Reference<GenericEntityMO> toReference(GenericEntityMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
