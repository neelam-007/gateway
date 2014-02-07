package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.UserRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ListingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ReadingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResourceUtils;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.util.Functions;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

/**
 * This resource handles user operations.
 *
 */

public class UserResource implements ListingResource<UserMO>, ReadingResource<UserMO> {

    @SpringBean
    private UserRestResourceFactory userRestResourceFactory;

    @Context
    private UriInfo uriInfo;

    //The provider id to manage version for.
    private String providerId;

    /**
     * Creates a new user resource for handling user requests for the given provider id
     *
     * @param providerId The provider the users belongs to.
     */
    public UserResource(String providerId) {
        this.providerId = providerId;
    }

    @Override
    public ItemsList<UserMO> listResources(final int offset, final int count, final String sort, final String order) {
        final String sortKey = userRestResourceFactory.getSortKey(sort);
        if (sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Item<UserMO>> items = Functions.map(userRestResourceFactory.listResources(providerId, offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(userRestResourceFactory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Item<UserMO>, UserMO>() {
            @Override
            public Item<UserMO> call(UserMO resource) {
                return toReference(resource);
            }
        });
        return new ItemsListBuilder<UserMO>(EntityType.USER + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    private Item<UserMO> toReference(UserMO resource) {
        return new ItemBuilder<UserMO>(resource.getId(), resource.getId(), EntityType.USER.name())
                .addLink(ManagedObjectFactory.createLink("self",getUsersUri(resource.getId()).toString()))
                .build();
    }

    private URI getUsersUri(String id) {
        String path = uriInfo.getBaseUriBuilder().path(IdentityProviderResource.class).build().toString();
        path = path + "/"+ providerId + "/users/" + id;
        return UriBuilder.fromPath(path).build();
    }

    @Override
    public Item<UserMO> getResource(String id)  throws ResourceFactory.ResourceNotFoundException, FindException {
        UserMO user = userRestResourceFactory.getResource(providerId, id);
        return new ItemBuilder<>(toReference(user))
                .setContent(user)
                .addLink(ManagedObjectFactory.createLink("list", getUsersUri(null).toString()))
                .build();
    }

    public Item toReference(IdentityHeader entityHeader) {
        return new ItemBuilder<UserMO>(entityHeader.getName(), entityHeader.getStrId(), EntityType.USER.name())
                .addLink(ManagedObjectFactory.createLink("self", getUsersUri(entityHeader.getName()).toString()))
                .build();
    }
}
