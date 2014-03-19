package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.GroupRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.GroupTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;

/**
 * This resource handles policy version operations.
 */
@Path(RestEntityResource.RestEntityResource_version_URI + "groups")
public class GroupResource implements URLAccessible<GroupMO> {

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

    /**
     * This will return a list of entity references. It will return a maximum of {@code count} references, it can return
     * fewer references if there are fewer then {@code count} entities found. Setting an offset will start listing
     * entities from the given offset. A sort can be specified to allow the resulting list to be sorted in either
     * ascending or descending order. Other params given will be used as search values. Examples:
     * <p/>
     * /restman/services?name=MyService
     * <p/>
     * Returns services with name = "MyService"
     * <p/>
     * /restman/storedpasswords?type=password&name=DevPassword,ProdPassword
     * <p/>
     * Returns stored passwords of password type with name either "DevPassword" or "ProdPassword"
     * <p/>
     * If a parameter is not a valid search value it will be ignored.
     *
     * @param offset The offset to start the listing from
     * @param count  The offset ot start the listing from
     * @param sort   the key to sort the list by.
     * @param order  the order to sort the list. true for ascending, false for descending. null implies ascending
     * @param names  The name filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<GroupMO> listResources(
            @QueryParam("offset") @DefaultValue("0") @NotEmpty Integer offset,
            @QueryParam("count") @DefaultValue("100") @NotEmpty Integer count,
            @QueryParam("sort") @ChoiceParam({"name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names) {
        ParameterValidationUtils.validateOffsetCount(offset, count);
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("name", "enabled", "type", "hardwiredServiceId", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }

        List<Item<GroupMO>> items = Functions.map(groupRestResourceFactory.listResources(providerId, offset, count, sort, ascendingSort, filters.map()), new Functions.Unary<Item<GroupMO>, GroupMO>() {
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

    public Item<GroupMO> getResource(String id) throws ResourceFactory.ResourceNotFoundException, FindException {
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
    public String getUrl(@NotNull EntityHeader groupHeader) {
        return getGroupUri(groupHeader.getName());
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
     *
     * @param id The id of the resource. Leave it blank to get the resource listing url
     * @return The url of the resource
     */
    public String getUrlString(@Nullable String id) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(this.getClass());
        if (id != null) {
            uriBuilder.path(id);
        }
        return uriBuilder.build().toString();
    }
}
