package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.AssertionSecurityZoneRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.List;

/**
 * The active connector resource
 */
@Provider
@Path(AssertionSecurityZoneResource.Version_URI + AssertionSecurityZoneResource.activeConnectors_URI)
public class AssertionSecurityZoneResource implements UpdatingResource<AssertionSecurityZoneMO>, ReadingResource<AssertionSecurityZoneMO>, ListingResource<AssertionSecurityZoneMO>, TemplatingResource<AssertionSecurityZoneMO> {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;
    protected static final String activeConnectors_URI = "assertionSecurityZones";
    protected AssertionSecurityZoneRestResourceFactory factory;
    @Context
    protected UriInfo uriInfo;

    @SpringBean
    public void setFactory(AssertionSecurityZoneRestResourceFactory factory) {
        this.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.ASSERTION_ACCESS;
    }

    protected Item<AssertionSecurityZoneMO> toReference(AssertionSecurityZoneMO resource) {
        return new ItemBuilder<AssertionSecurityZoneMO>(resource.getName(), resource.getId(), getEntityType().name())
                .addLink(ManagedObjectFactory.createLink("self", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), resource.getName())))
                .build();
    }

    @Override
    public ItemsList<AssertionSecurityZoneMO> listResources(final int offset, final int count, final String sort, final String order)
    {
        final String sortKey = factory.getSortKey(sort);
        if(sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Item<AssertionSecurityZoneMO>> items = Functions.map(factory.listResources(sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(factory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Item<AssertionSecurityZoneMO>, AssertionSecurityZoneMO>() {
            @Override
            public Item<AssertionSecurityZoneMO> call(AssertionSecurityZoneMO resource) {
                return toReference(resource);
            }
        });
        return new ItemsListBuilder<AssertionSecurityZoneMO>(EntityType.ASSERTION_ACCESS + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
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
        return new ItemBuilder<>(toReference(resource))
                .setContent(resource)
                .addLink(ManagedObjectFactory.createLink("template", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), "template")))
                .addLink(ManagedObjectFactory.createLink("list", uriInfo.getBaseUriBuilder().path(this.getClass()).build().toString()))
                .build();
    }

    @Override
    public Item<AssertionSecurityZoneMO> getResourceTemplate() {
        AssertionSecurityZoneMO resource = factory.getResourceTemplate();
        return new ItemBuilder<AssertionSecurityZoneMO>(getEntityType() + " Template", getEntityType().toString())
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
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
        return Response.ok().entity(toReference(updatedResource)).build();
    }
}
