package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.AssertionSecurityZoneAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.AssertionSecurityZoneTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * The active connector resource
 */
@Provider
@Path(AssertionSecurityZoneResource.Version_URI + AssertionSecurityZoneResource.activeConnectors_URI)
public class AssertionSecurityZoneResource implements UpdatingResource<AssertionSecurityZoneMO>, ReadingResource<AssertionSecurityZoneMO>, ListingResource<AssertionSecurityZoneMO>, TemplatingResource<AssertionSecurityZoneMO>, URLAccessible<AssertionSecurityZoneMO> {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;
    protected static final String activeConnectors_URI = "assertionSecurityZones";
    private AssertionSecurityZoneAPIResourceFactory factory;
    private AssertionSecurityZoneTransformer transformer;
    @Context
    protected UriInfo uriInfo;

    @SpringBean
    public void setFactory(AssertionSecurityZoneAPIResourceFactory factory) {
        this.factory = factory;
    }

    @SpringBean
    public void setTransformer(AssertionSecurityZoneTransformer transformer) {
        this.transformer = transformer;
    }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.ASSERTION_ACCESS.toString();
    }

    @Override
    public ItemsList<AssertionSecurityZoneMO> listResources(final ListRequestParameters listRequestParameters) {
        ParameterValidationUtils.validateListRequestParameters(listRequestParameters, factory.getSortKeysMap(), factory.getFiltersInfo());
        List<Item<AssertionSecurityZoneMO>> items = Functions.map(factory.listResources(listRequestParameters.getSort(), listRequestParameters.getOrder(), listRequestParameters.getFiltersMap()), new Functions.Unary<Item<AssertionSecurityZoneMO>, AssertionSecurityZoneMO>() {
            @Override
            public Item<AssertionSecurityZoneMO> call(AssertionSecurityZoneMO resource) {
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<AssertionSecurityZoneMO>(EntityType.ASSERTION_ACCESS + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLinks(getRelatedLinks(null))
                .build();
    }

    /**
     * This implements the GET method to retrieve an entity by a given name.
     *
     * @param name The name of the entity to select
     * @return The selected entity.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @Override
    public Item<AssertionSecurityZoneMO> getResource(String name) throws ResourceFactory.ResourceNotFoundException {
        AssertionSecurityZoneMO resource = factory.getResourceByName(name);
        return new ItemBuilder<>(transformer.convertToItem(resource))
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build();
    }

    @Override
    public Item<AssertionSecurityZoneMO> getResourceTemplate() {
        AssertionSecurityZoneMO resource = factory.getResourceTemplate();
        return new ItemBuilder<AssertionSecurityZoneMO>(getResourceType() + " Template", getResourceType().toString())
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLinks(getRelatedLinks(resource))
                .setContent(resource)
                .build();

    }
    /**
     * Updates an existing entity
     *
     * @param resource The updated entity
     * @param name       The name of the entity to update
     * @return a reference to the newly updated entity.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @Override
    public Response updateResource(AssertionSecurityZoneMO resource, String name) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        AssertionSecurityZoneMO updatedResource = factory.updateResourceByName(name, resource);
        return Response.ok().entity(new ItemBuilder<>(transformer.convertToItem(updatedResource))
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(updatedResource))
                .build()).build();
    }

    @NotNull
    @Override
    public Link getLink(@NotNull AssertionSecurityZoneMO resource) {
        return ManagedObjectFactory.createLink("self", getUrl(resource));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable AssertionSecurityZoneMO resource) {
        return Arrays.asList(
                ManagedObjectFactory.createLink("template", getUrlString("template")),
                ManagedObjectFactory.createLink("list", getUrlString(null))
        );
    }

    @NotNull
    @Override
    public String getUrl(@NotNull AssertionSecurityZoneMO resource) {
        return getUrlString(resource.getName());
    }

    @NotNull
    @Override
    public String getUrl(@NotNull EntityHeader header) {
        return getUrlString(header.getName());
    }

    /**
     * Returns the Url of this resource with the given id
     * @param id The id of the resource. Leave it blank to get the resource listing url
     * @return The url of the resource
     */
    private String getUrlString(@Nullable String id) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(this.getClass());
        if(id != null) {
            uriBuilder.path(id);
        }
        return uriBuilder.build().toString();
    }
}
