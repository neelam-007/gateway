package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.RevocationCheckingPolicyAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.RevocationCheckingPolicyTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;

/**
 * The active connector resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + RevocationCheckingPolicyResource.revocationCheckingPolicies_URI)
@Singleton
public class RevocationCheckingPolicyResource implements ReadingResource<RevocationCheckingPolicyMO>, ListingResource<RevocationCheckingPolicyMO>, URLAccessible<RevocationCheckingPolicyMO> {

    protected static final String revocationCheckingPolicies_URI = "revocationCheckingPolicies";
    private RevocationCheckingPolicyAPIResourceFactory factory;
    @Context
    protected UriInfo uriInfo;

    @SpringBean
    public void setFactory(RevocationCheckingPolicyAPIResourceFactory factory) {
        this.factory = factory;
    }

    @SpringBean
    private RevocationCheckingPolicyTransformer transformer;

    @Override
    public ItemsList<RevocationCheckingPolicyMO> listResources(final int offset, final int count, final String sort, final String order) {
        final String sortKey = factory.getSortKey(sort);
        if (sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Item<RevocationCheckingPolicyMO>> items = Functions.map(factory.listResources(offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(factory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Item<RevocationCheckingPolicyMO>, RevocationCheckingPolicyMO>() {
            @Override
            public Item<RevocationCheckingPolicyMO> call(RevocationCheckingPolicyMO resource) {
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<RevocationCheckingPolicyMO>(factory.getResourceType() + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLinks(getRelatedLinks(null))
                .build();
    }

    @Override
    public Item<RevocationCheckingPolicyMO> getResource(String id) throws ResourceFactory.ResourceNotFoundException {
        RevocationCheckingPolicyMO resource = factory.getResource(id);
        return new ItemBuilder<>(transformer.convertToItem(resource))
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build();
    }

    @NotNull
    @Override
    public String getResourceType() {
        return factory.getResourceType().toString();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull RevocationCheckingPolicyMO resource) {
        return getUrlString(resource.getId());
    }

    @NotNull
    @Override
    public Link getLink(@NotNull RevocationCheckingPolicyMO resource) {
        return ManagedObjectFactory.createLink("self", getUrl(resource));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable RevocationCheckingPolicyMO resource) {
        return Arrays.asList(
                ManagedObjectFactory.createLink("template", getUrlString("template")),
                ManagedObjectFactory.createLink("list", getUrlString(null)));
    }

    public String getUrlString(@Nullable String id) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(this.getClass());
        if(id != null) {
            uriBuilder.path(id);
        }
        return uriBuilder.build().toString();
    }
}
