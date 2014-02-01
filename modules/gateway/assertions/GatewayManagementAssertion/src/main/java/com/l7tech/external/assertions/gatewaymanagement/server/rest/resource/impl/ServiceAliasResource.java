package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServiceAliasRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.EntityHeader;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The service alias resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + ServiceAliasResource.publishedServiceAlias_URI)
@Singleton
public class ServiceAliasResource extends RestEntityResource<ServiceAliasMO, ServiceAliasRestResourceFactory> {

    protected static final String publishedServiceAlias_URI = "serviceAliases";

    @Override
    @SpringBean
    public void setFactory(ServiceAliasRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Item<ServiceAliasMO> toReference(ServiceAliasMO resource) {
        return toReference(resource.getId(), resource.getServiceReference().getId());
    }

    @Override
    public Item<ServiceAliasMO> toReference(EntityHeader entityHeader) {
        if (entityHeader instanceof AliasHeader) {
            return toReference(entityHeader.getStrId(),((AliasHeader) entityHeader).getAliasedEntityId().toString());
        } else {
            return super.toReference(entityHeader);
        }
    }
}
