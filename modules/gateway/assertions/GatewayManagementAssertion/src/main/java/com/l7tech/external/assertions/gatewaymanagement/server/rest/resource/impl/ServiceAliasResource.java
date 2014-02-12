package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServiceAliasAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ServiceAliasTransformer;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.gateway.rest.SpringBean;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The service alias resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + ServiceAliasResource.publishedServiceAlias_URI)
@Singleton
public class ServiceAliasResource extends RestEntityResource<ServiceAliasMO, ServiceAliasAPIResourceFactory, ServiceAliasTransformer> {

    protected static final String publishedServiceAlias_URI = "serviceAliases";

    @Override
    @SpringBean
    public void setFactory(ServiceAliasAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(ServiceAliasTransformer transformer) {
        super.transformer = transformer;
    }

    @NotNull
    @Override
    public String getUrl(@NotNull ServiceAliasMO serviceAliasMO) {
        return getUrlString(serviceAliasMO.getId());
    }
}
