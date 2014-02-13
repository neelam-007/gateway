package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.UserRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ListingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ReadingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResourceUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.UserTransformer;
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
 * This resource handles user operations.
 *
 */
@Path("users")
public class UserResource implements ListingResource<UserMO>, ReadingResource<UserMO>, URLAccessible<UserMO> {

    @SpringBean
    private UserRestResourceFactory userRestResourceFactory;

    @SpringBean
    private UserTransformer transformer;

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
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<UserMO>(EntityType.USER + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    @Override
    public Item<UserMO> getResource(String id)  throws ResourceFactory.ResourceNotFoundException, FindException {
        UserMO user = userRestResourceFactory.getResource(providerId, id);
        return new ItemBuilder<>(transformer.convertToItem(user))
                .addLink(getLink(user))
                .addLinks(getRelatedLinks(user))
                .build();
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.USER.toString();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull UserMO user) {
        //TODO: check if the user is from the default identity provider.
        return getUrlString(user.getLogin());
    }

    @NotNull
    @Override
    public Link getLink(@NotNull UserMO user) {
        return ManagedObjectFactory.createLink("self", getUrl(user));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable UserMO user) {
        //TODO: check if the user is from the default identity provider.
        return Arrays.asList(ManagedObjectFactory.createLink("list", getUrlString(null)));
    }

    public String getUrlString(@Nullable String id) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(this.getClass());
        if(id != null) {
            uriBuilder.path(id);
        }
        return uriBuilder.build().toString();
    }
}
