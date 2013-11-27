package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.Reference;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.TemplateFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * This is the base resource factory for a rest entity. It supports all crud operations:
 * <pre><ul>
 *     <li>Create</li>
 *     <li>Create with id</li>
 *     <li>Get</li>
 *     <li>delete</li>
 *     <li>update</li>
 *     <li>list. can specify offset, count and filters.</li>
 * </ul></pre>
 *
 * @author Victor Kazakov
 */
public abstract class RestEntityResource<R, F extends RestResourceFactory<R> & TemplateFactory<R>> implements CreatingResource<R>, ReadingResource, UpdatingResource<R>, DeletingResource, ListingResource, TemplatingResource {
    /**
     * This is the rest resource factory method used to perform the crud operations on the entity.
     */
    protected F factory;

    /**
     * This method needs to be called to set the factory. It should be called in the initialization faze before any of
     * the Rest methods are called. It should likely be annotated with {@link com.l7tech.gateway.rest.SpringBean} to
     * have jersey automatically inject the factory dependency
     *
     * @param factory The factory for this resource
     */
    @SuppressWarnings("UnusedDeclaration")
    public abstract void setFactory(F factory);

    @Override
    public Response listResources(UriInfo uriInfo, final int offset, final int count, final String sort, final String order) {
        final String sortKey = factory.getSortKey(sort);
        if(sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        return RestEntityResourceUtils.createReferenceListResponse(uriInfo.getAbsolutePath(), factory.listResources(offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(factory.getFiltersInfo(), uriInfo.getQueryParameters())));
    }

    @Override
    public Response getResource(String id) throws ResourceFactory.ResourceNotFoundException {
        R resource = factory.getResource(id);
        return Response.ok(resource).build();
    }

    @Override
    public Response getResourceTemplate() {
        R resource = factory.getResourceTemplate();
        return Response.ok(resource).build();
    }

    @Override
    public Response createResource(UriInfo uriInfo, R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        String id = factory.createResource(resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(id);
        final URI uri = ub.build();
        return Response.created(uri).entity(new Reference(uri.toString(), uri.toString())).build();
    }

    @Override
    public Response createResourceWithId(UriInfo uriInfo, R resource, String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.createResource(id, resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        final URI uri = ub.build();
        return Response.created(uri).entity(new Reference(uri.toString(), uri.toString())).build();
    }

    @Override
    public Response updateResource(UriInfo uriInfo, R resource, String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.updateResource(id, resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        final URI uri = ub.build();
        return Response.created(uri).entity(new Reference(uri.toString(), uri.toString())).build();
    }

    @Override
    public void deleteResource(String id) throws ResourceFactory.ResourceNotFoundException {
        factory.deleteResource(id);
    }
}
