package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServiceAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PublishedServiceTransformer;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;
import java.net.URI;
import java.util.logging.Logger;

/**
 * The published service resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + PublishedServiceResource.SERVICES_URI)
@Singleton
public class PublishedServiceResource extends DependentRestEntityResource<ServiceMO, ServiceAPIResourceFactory, PublishedServiceTransformer> {

    protected static final String SERVICES_URI = "services";
    private static final Logger logger = Logger.getLogger(PublishedServiceResource.class.getName());

    @Context
    private ResourceContext resourceContext;

    @Override
    @SpringBean
    public void setFactory( ServiceAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(PublishedServiceTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Shows the policy versions
     *
     * @param id The policy id
     * @return The policyVersion resource for handling policy version requests.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @Path("{id}/versions")
    public PolicyVersionResource versions(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        String policyId = factory.getPolicyIdForService(id);
        if(policyId != null){
            return resourceContext.initResource(new PolicyVersionResource(policyId));
        }
        throw new ResourceFactory.ResourceNotFoundException("Resource not found: " + id);
    }

    @Override
    public Response createResource(ServiceMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException{
        String comment = uriInfo.getQueryParameters().getFirst("versionComment");

        String id = factory.createResource(resource, comment);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(id);
        final URI uri = ub.build();
        return Response.created(uri).entity(new ItemBuilder<>(
                transformer.convertToItem(resource))
                .setContent(null)
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build())
                .build();
    }

    @Override
    public Response updateResource(ServiceMO resource, String id ) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException{

        String comment = uriInfo.getQueryParameters().getFirst("versionComment");
        String activeStr = uriInfo.getQueryParameters().getFirst("active");
        boolean active = activeStr == null ? true: Boolean.parseBoolean(activeStr);

        boolean resourceExists = factory.resourceExists(id);
        final Response.ResponseBuilder responseBuilder;
        if (resourceExists) {
            factory.updateResource(id, resource, comment, active);
            responseBuilder = Response.ok();
        } else {
            factory.createResource(id, resource, comment);
            responseBuilder = Response.created(uriInfo.getAbsolutePath());
        }
        return responseBuilder.entity(new ItemBuilder<>(
                transformer.convertToItem(resource))
                .setContent(null)
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build()).build();
    }
}
