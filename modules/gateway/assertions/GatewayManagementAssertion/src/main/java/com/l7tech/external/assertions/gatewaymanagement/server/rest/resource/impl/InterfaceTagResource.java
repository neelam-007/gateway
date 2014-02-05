package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.InterfaceTagRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * The interface tag resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + InterfaceTagResource.interfaceTags_URI)
@Singleton
public class InterfaceTagResource extends RestEntityResource<InterfaceTagMO, InterfaceTagRestResourceFactory> {

    protected static final String interfaceTags_URI = "interfaceTags";

    @Override
    @SpringBean
    public void setFactory(InterfaceTagRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Item<InterfaceTagMO> toReference(InterfaceTagMO resource) {
        return toReference(resource.getId(), resource.getName());
    }

    @Override
    public Response updateResource(InterfaceTagMO resource, String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.updateResource(id, resource);
        return Response.ok().entity(toReference(resource)).build();
    }
}
