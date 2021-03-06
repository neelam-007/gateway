package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.GroupRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.GroupTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * A group represents a group identity in an identity provider. When no identity provider is specified in the url then
 * the internal identity provider is assumed. Groups can only be retrieved, they can not be created.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + GroupResource.GROUPS_URI)
public class GroupResource implements URLAccessible<GroupMO> {

    protected static final String GROUPS_URI = "groups";

    @SpringBean
    private GroupRestResourceFactory groupRestResourceFactory;

    @SpringBean
    private GroupTransformer transformer;

    @Context
    private UriInfo uriInfo;

    //The provider id to manage version for.
    private final String providerId;

    /**
     * Creates a group resource for handling group request for the internal identity provider
     */
    public GroupResource() {
        providerId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();
    }

    /**
     * Creates a new group resource for handling group requests for the given provider id
     *
     * @param providerId The provider the group belongs to.
     */
    public GroupResource(String providerId) {
        this.providerId = providerId;
    }

    /**
     * <p>Returns a list of groups. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort  Key to sort the list by.
     * @param order Sort order for the list; 'asc'=ascending, 'desc'=descending; defaults to
     *              ascending if not specified
     * @param names Name filter
     * @return A list of groups. If the list is empty then no groups were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<GroupMO> listGroups(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names) throws ResourceFactory.ResourceNotFoundException {

        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        return RestEntityResourceUtils.createItemsList(groupRestResourceFactory.listResources(sort, ascendingSort, providerId, filters.map()), transformer, this, uriInfo.getRequestUri().toString());
    }

    /**
     * Returns a group with the given ID.
     *
     * @param id The ID of the group to return
     * @return The group.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{groupID}")
    public Item<GroupMO> getGroup(@PathParam("groupID") String id) throws ResourceFactory.ResourceNotFoundException, FindException {
        GroupMO group = groupRestResourceFactory.getResource(providerId, id);
        return RestEntityResourceUtils.createGetResponseItem(group, transformer, this);
    }

    /**
     * Returns a template, which is an example group that can be used as a reference for what group objects should look
     * like.
     *
     * @return The template group.
     */
    @GET
    @Path("template")
    public Item<GroupMO> groupTemplate() {
        GroupMO groupMO = ManagedObjectFactory.createGroupMO();
        groupMO.setProviderId(providerId);
        groupMO.setName("Name");
        return RestEntityResourceUtils.createTemplateItem(groupMO, this, getUrlString(providerId, "template"));
    }


    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.GROUP.toString();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull GroupMO group) {
        return getUrlString(group.getProviderId(), group.getId());
    }

    @NotNull
    @Override
    public String getUrl(@NotNull EntityHeader groupHeader) {
        if (groupHeader instanceof IdentityHeader) {
            return getUrlString(((IdentityHeader) groupHeader).getProviderGoid().toString(), groupHeader.getStrId());
        }
        return getUrlString(providerId, groupHeader.getStrId());
    }

    @NotNull
    @Override
    public Link getLink(@NotNull GroupMO group) {
        return ManagedObjectFactory.createLink(Link.LINK_REL_SELF, getUrl(group));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable GroupMO group) {
        ArrayList<Link> links = new ArrayList<>();
        links.add(ManagedObjectFactory.createLink(Link.LINK_REL_TEMPLATE, getUrlString(providerId, "template")));
        links.add(ManagedObjectFactory.createLink(Link.LINK_REL_LIST, getUrlString(providerId, null)));
        if (group != null && group.getProviderId() != null) {
            links.add(ManagedObjectFactory.createLink("provider", uriInfo.getBaseUriBuilder()
                    .path(IdentityProviderResource.class)
                    .path(providerId).build().toString()));
        }
        return links;
    }

    /**
     * Returns the Url of this resource with the given id
     *
     * @param providerId The id of the identity provider that the group belongs to.
     * @param groupId    The id of the resource. Leave it blank to get the resource listing url
     * @return The url of the resource
     */
    private String getUrlString(String providerId, @Nullable String groupId) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder()
                .path(IdentityProviderResource.class)
                .path(providerId)
                .path(GROUPS_URI);
        if (groupId != null) {
            uriBuilder.path(groupId);
        }
        return uriBuilder.build().toString();
    }
}
