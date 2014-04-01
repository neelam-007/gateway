package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.TemplateFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
public abstract class RestEntityResource<R, F extends APIResourceFactory<R> & TemplateFactory<R>, T extends APITransformer<R, ?>> implements URLAccessible<R> {
    public static final String RestEntityResource_version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;

    /**
     * This is the rest resource factory method used to perform the crud operations on the entity.
     */
    protected F factory;

    protected T transformer;

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
     * This method needs to be called to set the transformer. It should be called in the initialization faze before any
     * of the Rest methods are called. It should likely be annotated with {@link com.l7tech.gateway.rest.SpringBean} to
     * have jersey automatically inject the transformer dependency
     *
     * @param transformer The transformer for this resource
     */
    @SuppressWarnings("UnusedDeclaration")
    public abstract void setTransformer(T transformer);

    /**
     * Returns the entity type of the resource
     *
     * @return The resource entity type
     */
    @Override
    @NotNull
    public String getResourceType() {
        return factory.getResourceType();
    }

    public ItemsList<R> listResources(final String sort, final Boolean asc, final Map<String, List<Object>> filters) {
        List<Item<R>> items = Functions.map(factory.listResources(sort, asc, filters), new Functions.Unary<Item<R>, R>() {
            @Override
            public Item<R> call(R resource) {
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<R>(getResourceType() + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLinks(getRelatedLinks(null))
                .build();
    }

    /**
     * Returns the Url of this resource with the given id
     *
     * @param id The id of the resource. Leave it blank to get the resource listing url
     * @return The url of the resource
     */
    @NotNull
    protected String getUrlString(@Nullable String id) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(this.getClass());
        if (id != null) {
            uriBuilder.path(id);
        }
        return uriBuilder.build().toString();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull R resource) {
        if(resource instanceof ManagedObject){
            return getUrlString(((ManagedObject)resource).getId());
        }
        //In this case the getUrl method should have been overriden by the specific entityResource
        throw new IllegalArgumentException("Cannot get url for a non managed object resource: " + resource.getClass());
    }

    @NotNull
    @Override
    public String getUrl(@NotNull EntityHeader header) {
        return getUrlString(header.getStrId());
    }

    @NotNull
    @Override
    public Link getLink(@NotNull R resource) {
        return ManagedObjectFactory.createLink("self", getUrl(resource));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable R resource) {
        return Arrays.asList(
                ManagedObjectFactory.createLink("template", getUrlString("template")),
                ManagedObjectFactory.createLink("list", getUrlString(null))
        );
    }

    public Item<R> getResource(String id) throws ResourceFactory.ResourceNotFoundException {
        R resource = factory.getResource(id);
        return new ItemBuilder<>(transformer.convertToItem(resource))
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build();
    }

    public Item<R> getResourceTemplate() {
        R resource = factory.getResourceTemplate();
        return new ItemBuilder<R>(getResourceType() + " Template", getResourceType())
                .addLink(ManagedObjectFactory.createLink("self", getUrlString("template")))
                .addLinks(getRelatedLinks(resource))
                .setContent(resource)
                .build();
    }

    public Response createResource(R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        String id = factory.createResource(resource);
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

    public Response updateResource(R resource, String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        boolean resourceExists = factory.resourceExists(id);
        final Response.ResponseBuilder responseBuilder;
        if (resourceExists) {
            factory.updateResource(id, resource);
            responseBuilder = Response.ok();
        } else {
            factory.createResource(id, resource);
            responseBuilder = Response.created(uriInfo.getAbsolutePath());
        }
        return responseBuilder.entity(new ItemBuilder<>(
                transformer.convertToItem(resource))
                .setContent(null)
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build()).build();
    }

    public void deleteResource(String id) throws ResourceFactory.ResourceNotFoundException {
        factory.deleteResource(id);
    }
}
