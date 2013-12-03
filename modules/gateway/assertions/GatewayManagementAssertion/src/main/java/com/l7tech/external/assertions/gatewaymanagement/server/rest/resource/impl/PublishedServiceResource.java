package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServiceRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The published service resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(PublishedServiceResource.SERVICES_URI)
public class PublishedServiceResource extends DependentRestEntityResource<ServiceMO, ServiceRestResourceFactory> {

    protected static final String SERVICES_URI = "services";

    @Override
    @SpringBean
    public void setFactory( ServiceRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.SERVICE;
    }

    @Override
    protected Reference toReference(ServiceMO resource) {
        return toReference(resource.getId(), resource.getServiceDetail().getName());
    }
}
