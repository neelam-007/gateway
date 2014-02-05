package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.RevocationCheckingPolicyRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ListingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ReadingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResourceUtils;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.Functions;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.List;

/**
 * The active connector resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + RevocationCheckingPolicyResource.revocationCheckingPolicies_URI)
@Singleton
public class RevocationCheckingPolicyResource implements ReadingResource<RevocationCheckingPolicyMO>, ListingResource<RevocationCheckingPolicyMO> {

    protected static final String revocationCheckingPolicies_URI = "revocationCheckingPolicies";
    private RevocationCheckingPolicyRestResourceFactory factory;
    @Context
    protected UriInfo uriInfo;

    @SpringBean
    public void setFactory(RevocationCheckingPolicyRestResourceFactory factory) {
        this.factory = factory;
    }

    protected Item<RevocationCheckingPolicyMO> toReference(RevocationCheckingPolicyMO resource) {
        return new ItemBuilder<RevocationCheckingPolicyMO>(resource.getName(), resource.getId(), factory.getEntityType().name())
                .addLink(ManagedObjectFactory.createLink("self", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(),  resource.getId())))
                .build();
    }

    @Override
    public ItemsList<RevocationCheckingPolicyMO> listResources(final int offset, final int count, final String sort, final String order) {
        final String sortKey = factory.getSortKey(sort);
        if (sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Item<RevocationCheckingPolicyMO>> items = Functions.map(factory.listResources(offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(factory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Item<RevocationCheckingPolicyMO>, RevocationCheckingPolicyMO>() {
            @Override
            public Item<RevocationCheckingPolicyMO> call(RevocationCheckingPolicyMO resource) {
                return toReference(resource);
            }
        });
        return new ItemsListBuilder<RevocationCheckingPolicyMO>(factory.getEntityType() + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    @Override
    public Item<RevocationCheckingPolicyMO> getResource(String id) throws ResourceFactory.ResourceNotFoundException {
        RevocationCheckingPolicyMO resource = factory.getResource(id);
        return new ItemBuilder<>(toReference(resource))
                .setContent(resource)
                .addLink(ManagedObjectFactory.createLink("template", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), "template")))
                .addLink(ManagedObjectFactory.createLink("list", uriInfo.getBaseUriBuilder().path(this.getClass()).build().toString()))
                .build();
    }
}
