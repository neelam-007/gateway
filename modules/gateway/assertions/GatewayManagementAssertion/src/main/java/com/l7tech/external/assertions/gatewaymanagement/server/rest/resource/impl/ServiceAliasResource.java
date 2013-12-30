package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServiceAliasRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The service alias resource
 */
@Provider
@Path(ServiceAliasResource.publishedServiceAlias_URI)
public class ServiceAliasResource extends RestEntityResource<ServiceAliasMO, ServiceAliasRestResourceFactory> {

    protected static final String publishedServiceAlias_URI = "serviceAliases";

    @Override
    @SpringBean
    public void setFactory(ServiceAliasRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.SERVICE_ALIAS;
    }

    @Override
    protected Reference toReference(ServiceAliasMO resource) {
        return toReference(resource.getId(), resource.getServiceReference().getId());
    }
}
