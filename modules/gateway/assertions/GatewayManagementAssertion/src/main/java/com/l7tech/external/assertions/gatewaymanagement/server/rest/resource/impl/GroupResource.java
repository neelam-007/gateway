package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.GroupRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ListingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ReadingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResourceUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.GroupTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;

/**
 * This resource handles policy version operations.
 */
@Path("groups")
public class GroupResource implements ListingResource<GroupMO>, ReadingResource<GroupMO>, URLAccessible<GroupMO> {

    @SpringBean
    private GroupRestResourceFactory groupRestResourceFactory;

    @SpringBean
    private GroupTransformer transformer;

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
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<GroupMO>(EntityType.GROUP + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLinks(getRelatedLinks(null))
                .build();
    }

    private String getGroupUri(String id) {
        return getUrlString(providerId + "/groups/" + id);
    }

    @Override
    public Item<GroupMO> getResource(String id)  throws ResourceFactory.ResourceNotFoundException, FindException {
        GroupMO group = groupRestResourceFactory.getResource(providerId, id);
        return new ItemBuilder<>(transformer.convertToItem(group))
                .addLink(getLink(group))
                .addLinks(getRelatedLinks(group))
                .build();
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.GROUP.toString();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull GroupMO group) {
        return getGroupUri(group.getName());
    }

    @NotNull
    @Override
    public Link getLink(@NotNull GroupMO group) {
        return ManagedObjectFactory.createLink("self", getUrl(group));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable GroupMO group) {
        return Arrays.asList(ManagedObjectFactory.createLink("list", getGroupUri(null)));
    }

    /**
     * Returns the Url of this resource with the given id
     * @param id The id of the resource. Leave it blank to get the resource listing url
     * @return The url of the resource
     */
    public String getUrlString(@Nullable String id) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(this.getClass());
        if(id != null) {
            uriBuilder.path(id);
        }
        return uriBuilder.build().toString();
    }
}
