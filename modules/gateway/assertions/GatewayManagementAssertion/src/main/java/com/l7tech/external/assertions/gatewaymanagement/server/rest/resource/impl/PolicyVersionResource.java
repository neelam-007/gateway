package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyVersionRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.NotEmpty;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyVersionTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.glassfish.jersey.message.XmlHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Arrays;
import java.util.List;

/**
 * This resource handles policy version operations.
 *
 * @author Victor Kazakov
 */
public class PolicyVersionResource implements URLAccessible<PolicyVersionMO> {

    @SpringBean
    private PolicyVersionRestResourceFactory policyVersionRestResourceFactory;

    @SpringBean
    private PolicyVersionTransformer transformer;

    @Context
    private UriInfo uriInfo;

    //The policy id to manage version for.
    private String policyId;

    /**
     * Creates a new policy version resource for handling policy version requests for the given policy
     *
     * @param policyId The policy to handle policy version requests for.
     */
    public PolicyVersionResource(String policyId) {
        this.policyId = policyId;
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
     * @param offset   The offset to start the listing from
     * @param count    The offset ot start the listing from
     * @param sort     the key to sort the list by.
     * @param order    the order to sort the list. true for ascending, false for descending. null implies ascending
     * @param versions The version filter
     * @param active   the enabled filter
     * @param comments The comment filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    //@XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public ItemsList<PolicyVersionMO> listResources(
            @QueryParam("offset") @DefaultValue("0") @NotEmpty Integer offset,
            @QueryParam("count") @DefaultValue("100") @NotEmpty Integer count,
            @QueryParam("sort") @ChoiceParam({"id", "version"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("version") List<Long> versions,
            @QueryParam("active") Boolean active,
            @QueryParam("comment") List<String> comments) {
        ParameterValidationUtils.validateOffsetCount(offset, count);
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("version", "active", "comment"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (versions != null && !versions.isEmpty()) {
            filters.put("ordinal", (List) versions);
        }
        if (active != null) {
            filters.put("active", (List) Arrays.asList(active));
        }
        if (comments != null && !comments.isEmpty()) {
            filters.put("name", (List) comments);
        }
        List<Item<PolicyVersionMO>> items = Functions.map(policyVersionRestResourceFactory.listResources(policyId, offset, count, sort, ascendingSort, filters.map()), new Functions.Unary<Item<PolicyVersionMO>, PolicyVersionMO>() {
            @Override
            public Item<PolicyVersionMO> call(PolicyVersionMO resource) {
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<PolicyVersionMO>(EntityType.POLICY_VERSION + " list", "List").setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    /**
     * This implements the GET method to retrieve an entity by a given id.
     *
     * @param id The identity of the entity to select
     * @return The selected entity.
     */
    @GET
    @Path("{id}")
    public Item<PolicyVersionMO> getResource(@PathParam("id") String id) throws FindException {
        PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getResource(policyId, id);
        return new ItemBuilder<>(transformer.convertToItem(policyVersion))
                .addLink(getLink(policyVersion))
                .addLinks(getRelatedLinks(policyVersion))
                .build();
    }

    /**
     * Returns the active policy version
     *
     * @return The active policy version.
     * @throws FindException
     */
    @GET
    @Path("active")
    public Item<PolicyVersionMO> getActiveVersion() throws FindException {
        PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getActiveVersion(policyId);
        return new ItemBuilder<>(transformer.convertToItem(policyVersion))
                .addLink(getLink(policyVersion))
                .addLinks(getRelatedLinks(policyVersion))
                .build();
    }

    /**
     * Sets the comment on a specific policy version.
     *
     * @param id      The id of thw policy version to set the comment on.
     * @param comment The comment to set on the policy version. This will override any existing comment
     * @return A reference to the policy version
     */
    @PUT
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("{id}/comment")
    public Response setComment(@PathParam("id") String id, String comment) throws FindException, UpdateException {
        policyVersionRestResourceFactory.updateComment(policyId, id, comment);
        PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getResource(policyId, id);
        return Response.ok(new ItemBuilder<>(transformer.convertToItem(policyVersion))
                .addLink(getLink(policyVersion))
                .addLinks(getRelatedLinks(policyVersion))
                .build()).build();
    }

    /**
     * Setts a comment on the active policy version
     *
     * @param comment The comment to set on the active policy version
     * @return The active policy version
     * @throws FindException
     * @throws UpdateException
     */
    @PUT
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("active/comment")
    public Response setComment(String comment) throws FindException, UpdateException {
        policyVersionRestResourceFactory.updateActiveComment(policyId, comment);
        PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getActiveVersion(policyId);
        return Response.ok(new ItemBuilder<>(transformer.convertToItem(policyVersion))
                .addLink(getLink(policyVersion))
                .addLinks(getRelatedLinks(policyVersion))
                .build()).build();
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.POLICY_VERSION.toString();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull PolicyVersionMO policyVersion) {
        return getUrlString(Long.toString(policyVersion.getOrdinal()));
    }

    @NotNull
    @Override
    public String getUrl(@NotNull EntityHeader policyVersionHeader) {
        throw new UnsupportedOperationException("Cannot get the url of a policy version from its entity header.");
    }

    @NotNull
    @Override
    public Link getLink(@NotNull PolicyVersionMO policyVersion) {
        return ManagedObjectFactory.createLink("self", getUrl(policyVersion));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable PolicyVersionMO policyVersion) {
        return Arrays.asList(
                ManagedObjectFactory.createLink("template", getUrlString("template")),
                ManagedObjectFactory.createLink("list", getUrlString(null)));
    }

    public String getUrlString(@Nullable String id) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(PolicyResource.class).path(policyId).path("versions");
        if (id != null) {
            uriBuilder.path(id);
        }
        return uriBuilder.build().toString();
    }
}
