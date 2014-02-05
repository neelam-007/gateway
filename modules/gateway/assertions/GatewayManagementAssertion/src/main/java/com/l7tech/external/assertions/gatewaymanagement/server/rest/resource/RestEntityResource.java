package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.TemplateFactory;
import com.l7tech.gateway.api.*;
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
public abstract class RestEntityResource<R, F extends RestResourceFactory<R> & TemplateFactory<R>> implements CreatingResource<R>, ReadingResource<R>, UpdatingResource<R>, DeletingResource, ListingResource<R>, TemplatingResource<R> {
    public static final String RestEntityResource_version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;

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
     * This will return the factory that is used by this rest entity resource.
     *
     * @return The factory that is used by this rest entity
     */
    public F getFactory() {
        return factory;
    }

    /**
     * Returns the entity type of the resource
     *
     * @return The resource entity type
     */
    @NotNull
    public EntityType getEntityType() {
        return factory.getEntityType();
    }

    @Override
    public ItemsList<R> listResources(final int offset, final int count, final String sort, final String order) {
        final String sortKey = factory.getSortKey(sort);
        if (sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Item<R>> items = Functions.map(factory.listResources(offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(factory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Item<R>, R>() {
            @Override
            public Item<R> call(R resource) {
                return toReference(resource);
            }
        });
        return new ItemsListBuilder<R>(getEntityType() + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    protected abstract Item<R> toReference(R resource);

    public Item<R> toReference(EntityHeader entityHeader) {
        return toReference(entityHeader.getStrId(), entityHeader.getName());
    }

    protected Item<R> toReference(String id, String title) {
        return new ItemBuilder<R>(title, id, getEntityType().name())
                .addLink(ManagedObjectFactory.createLink("self", getUrl(id)))
                .build();
    }

    /**
     * Returns the Url of this resource with the given id
     * @param id The id of the resource. Leave it blank to get the resource listing url
     * @return The url of the resource
     */
    public String getUrl(String id) {
        return RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), id);
    }

    @Override
    public Item<R> getResource(String id) throws ResourceFactory.ResourceNotFoundException {
        R resource = factory.getResource(id);
        return new ItemBuilder<>(toReference(resource))
                .setContent(resource)
                .addLink(ManagedObjectFactory.createLink("template", getUrl("template")))
                .addLink(ManagedObjectFactory.createLink("list", uriInfo.getBaseUriBuilder().path(this.getClass()).build().toString()))
                .build();
    }

    @Override
    public Item<R> getResourceTemplate() {
        R resource = factory.getResourceTemplate();
        return new ItemBuilder<R>(getEntityType() + " Template", getEntityType().toString())
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .setContent(resource)
                .build();
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
        boolean resourceExists = factory.resourceExists(id);
        if (resourceExists) {
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
