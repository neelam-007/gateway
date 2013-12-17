package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.TemplateFactory;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.ReferenceBuilder;
import com.l7tech.gateway.api.References;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

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

    @Context
    protected UriInfo uriInfo;

    /**
     * This method needs to be called to set the factory. It should be called in the initialization faze before any of
     * the Rest methods are called. It should likely be annotated with {@link com.l7tech.gateway.rest.SpringBean} to
     * have jersey automatically inject the factory dependency
     *
     * @param factory The factory for this resource
     */
    @SuppressWarnings("UnusedDeclaration")
    public abstract void setFactory(F factory);

    /**
     * Returns the entity type of the resource
     *
     * @return The resource entity type
     */
    @NotNull
    public abstract EntityType getEntityType();

    @Override
    public References listResources(final int offset, final int count, final String sort, final String order) {
        final String sortKey = factory.getSortKey(sort);
        if(sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Reference> references = Functions.map(factory.listResources(offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(factory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Reference, R>() {
            @Override
            public Reference call(R resource) {
                return toReference(resource);
            }
        });
        return ManagedObjectFactory.createReferences(references);
    }

    protected abstract Reference toReference(R resource);

    public Reference toReference(EntityHeader entityHeader) {
        return toReference(entityHeader.getStrId(), entityHeader.getName());
    }

    protected Reference toReference(String id, String title){
        return new ReferenceBuilder(title, id, getEntityType().name())
                .addLink(ManagedObjectFactory.createLink("self", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), id)))
                .build();
    }

    @Override
    public Reference getResource(String id) throws ResourceFactory.ResourceNotFoundException {
        R resource = factory.getResource(id);
        return new ReferenceBuilder(toReference(resource))
                .setContent(resource)
                .addLink(ManagedObjectFactory.createLink("template", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), "template")))
                .addLink(ManagedObjectFactory.createLink("list", uriInfo.getBaseUriBuilder().path(this.getClass()).build().toString()))
                .build();
    }

    @Override
    public Response getResourceTemplate() {
        R resource = factory.getResourceTemplate();
        return Response.ok(resource).build();
    }

    @Override
    public Response createResource(R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        String id = factory.createResource(resource);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(id);
        final URI uri = ub.build();
        return Response.created(uri).entity(toReference(resource)).build();
    }

    @Override
    public Response updateResource(R resource, String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        R existingResource;
        try {
            existingResource = factory.getResource(id);
        } catch (ResourceFactory.ResourceNotFoundException e){
            existingResource = null;
        }
        if(existingResource != null){
            factory.updateResource(id, resource);
            return Response.ok().entity(toReference(resource)).build();
        } else {
            factory.createResource(id, resource);
            return Response.created(uriInfo.getAbsolutePath()).entity(toReference(resource)).build();
        }
    }

    @Override
    public void deleteResource(String id) throws ResourceFactory.ResourceNotFoundException {
        factory.deleteResource(id);
    }
}
