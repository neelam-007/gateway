package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.InterfaceTagAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.InterfaceTagTransformer;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.ItemBuilder;
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
public class InterfaceTagResource extends RestEntityResource<InterfaceTagMO, InterfaceTagAPIResourceFactory, InterfaceTagTransformer> {

    protected static final String interfaceTags_URI = "interfaceTags";

    @Override
    @SpringBean
    public void setFactory(InterfaceTagAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(InterfaceTagTransformer transformer) {
        super.transformer = transformer;
    }

    @Override
    public Response updateResource(InterfaceTagMO resource, String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.updateResource(id, resource);
        return Response.ok().entity(new ItemBuilder<>(transformer.convertToItem(resource))
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build()).build();
    }
}
