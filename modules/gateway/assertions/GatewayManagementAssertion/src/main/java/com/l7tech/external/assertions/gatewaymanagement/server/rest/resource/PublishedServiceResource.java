package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory;
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
public class PublishedServiceResource extends DependentRestEntityResource<ServiceMO, ServiceResourceFactory> {

    protected static final String SERVICES_URI = "services";

    @Override
    @SpringBean
    public void setFactory( ServiceResourceFactory factory) {
        super.factory = factory;
    }

    protected EntityType getEntityType(){
        return EntityType.SERVICE;
    }
}
