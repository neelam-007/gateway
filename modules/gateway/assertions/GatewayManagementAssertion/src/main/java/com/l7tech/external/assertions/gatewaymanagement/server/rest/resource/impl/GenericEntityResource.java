package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.GenericEntityAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.GenericEntityTransformer;
import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.gateway.rest.SpringBean;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The generic entity resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + GenericEntityResource.genericEntity_URI)
@Singleton
public class GenericEntityResource extends RestEntityResource<GenericEntityMO, GenericEntityAPIResourceFactory, GenericEntityTransformer> {

    protected static final String genericEntity_URI = "genericEntities";

    @Override
    @SpringBean
    public void setFactory(GenericEntityAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(GenericEntityTransformer transformer) {
        super.transformer = transformer;
    }

    @NotNull
    @Override
    public String getUrl(@NotNull GenericEntityMO genericEntityMO) {
        return getUrlString(genericEntityMO.getId());
    }
}
