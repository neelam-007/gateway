package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.GroupRestResourceFactory;
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
 * This resource handles policy version operations.
 */
public class GroupResource implements ListingResource<GroupMO>, ReadingResource<GroupMO> {

    @SpringBean
    private GroupRestResourceFactory groupRestResourceFactory;

    @Context
    private UriInfo uriInfo;

    //The provider id to manage version for.
    private String providerId;

    /**
     * Creates a new group resource for handling group requests for the given provider id
     *
     * @param providerId The provider the group belongs to.
     */
    public GroupResource(String providerId) {
        this.providerId = providerId;
    }

    @Override
    public ItemsList<GroupMO> listResources(final int offset, final int count, final String sort, final String order) {
        final String sortKey = groupRestResourceFactory.getSortKey(sort);
        if (sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Item<GroupMO>> items = Functions.map(groupRestResourceFactory.listResources(providerId, offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(groupRestResourceFactory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Item<GroupMO>, GroupMO>() {
            @Override
            public Item<GroupMO> call(GroupMO resource) {
                return toReference(resource);
            }
        });
        return new ItemsListBuilder<GroupMO>(EntityType.GROUP + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    private Item<GroupMO> toReference(GroupMO resource) {
        return new ItemBuilder<GroupMO>(resource.getName(), resource.getId(), EntityType.GROUP.name())
                .addLink(ManagedObjectFactory.createLink("self", getGroupUri(resource.getName()).toString()))
                .build();
    }

    private URI getGroupUri(String id) {
        String path = uriInfo.getBaseUriBuilder().path(IdentityProviderResource.class).build().toString();
        path = path + "/"+ providerId + "/groups/" + id;
        return UriBuilder.fromPath(path).build();
    }

    @Override
    public Item<GroupMO> getResource(String id)  throws ResourceFactory.ResourceNotFoundException, FindException {
        GroupMO group = groupRestResourceFactory.getResource(providerId, id);
        return new ItemBuilder<>(toReference(group))
                .setContent(group)
                .addLink(ManagedObjectFactory.createLink("list", getGroupUri(null).toString()))
                .build();
    }

    public Item toReference(IdentityHeader entityHeader) {
        return new ItemBuilder<UserMO>(entityHeader.getName(), entityHeader.getStrId(), EntityType.USER.name())
                .addLink(ManagedObjectFactory.createLink("self",  getGroupUri(entityHeader.getName()).toString()))
                .build();
    }

}
