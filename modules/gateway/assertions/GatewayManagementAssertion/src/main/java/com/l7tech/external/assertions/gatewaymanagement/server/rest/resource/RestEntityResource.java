package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
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
public abstract class RestEntityResource<R, F extends APIResourceFactory<R>, T extends APITransformer<R, ?>> implements URLAccessible<R> {
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

    /**
     * Gets the url to the specified resource. With the specified id.
     *
     * @param urlAccessibleClass The class of the resource to get the link to
     * @param id                 The id of the resource.
     * @return The url to access this resource.
     */
    @NotNull
    protected String getUrlString(@NotNull final Class<? extends URLAccessible> urlAccessibleClass, @NotNull final String id) {
        final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(urlAccessibleClass);
        uriBuilder.path(id);
        return uriBuilder.build().toString();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull R resource) {
        if (resource instanceof ManagedObject) {
            return getUrlString(((ManagedObject) resource).getId());
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
        return ManagedObjectFactory.createLink(Link.LINK_REL_SELF, getUrl(resource));
    }

    /**
     * Returns all related links for the given resource
     *
     * @param resource The resource to return related links for
     * @return The list of related links
     */
    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable R resource) {
        final ArrayList<Link> links = new ArrayList<>();
        links.add(ManagedObjectFactory.createLink(Link.LINK_REL_TEMPLATE, getUrlString("template")));
        links.add(ManagedObjectFactory.createLink(Link.LINK_REL_LIST, getUrlString(null)));
        return links;
    }

    /**
     * Retrieves a resource with the given id;
     *
     * @param id The id of the resource to retrieve
     * @return The resource with the given id
     * @throws ResourceFactory.ResourceNotFoundException
     */
    protected Item<R> get(String id) throws ResourceFactory.ResourceNotFoundException {
        R resource = factory.getResource(id);
        return RestEntityResourceUtils.createGetResponseItem(resource, transformer, this);
    }

    /**
     * Lists resources given a sort and filters.
     *
     * @param sort    The sort key to sort the reources by
     * @param asc     The sort order
     * @param filters The filters to use to filter list of resources.
     * @return The resulting resource list
     */
    protected ItemsList<R> list(final String sort, final Boolean asc, final Map<String, List<Object>> filters) {
        return RestEntityResourceUtils.createItemsList(
                factory.listResources(sort, asc, filters),
                transformer,
                this,
                uriInfo.getRequestUri().toString());
    }

    /**
     * Creates a template item given a template resource
     *
     * @param resource The template resource
     * @return The template item
     */
    protected Item<R> createTemplateItem(@NotNull final R resource) {
        return RestEntityResourceUtils.createTemplateItem(resource, this, getUrlString("template"));
    }

    /**
     * Creates the given resource
     *
     * @param resource The resource to create
     * @return The created response
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    protected Response create(R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.createResource(resource);
        return RestEntityResourceUtils.createCreateOrUpdatedResponseItem(resource, transformer, this, true);
    }

    /**
     * Updates the given resource. Or creates it if one with the given id does not exist.
     *
     * @param resource The resource to update
     * @param id       The id of the resource to update
     * @return The update or create response
     * @throws ResourceFactory.ResourceFactoryException
     */
    protected Response update(R resource, String id) throws ResourceFactory.ResourceFactoryException {
        boolean resourceExists = factory.resourceExists(id);
        if (resourceExists) {
            factory.updateResource(id, resource);
        } else {
            factory.createResource(id, resource);
        }
        return RestEntityResourceUtils.createCreateOrUpdatedResponseItem(resource, transformer, this, !resourceExists);

    }

    /**
     * Deletes a resource with the given id
     *
     * @param id The id of the resource to delete
     * @throws ResourceFactory.ResourceNotFoundException
     */
    protected void delete(String id) throws ResourceFactory.ResourceNotFoundException {
        factory.deleteResource(id);
    }
}
