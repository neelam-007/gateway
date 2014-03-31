package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
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
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Either;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This resource handles policy version operations.
 *
 * @author Victor Kazakov
 */
public class PolicyVersionResource implements URLAccessible<PolicyVersionMO> {

    protected static final String VERSIONS_URI = "versions";

    @SpringBean
    private PolicyVersionRestResourceFactory policyVersionRestResourceFactory;

    @SpringBean
    private PolicyVersionTransformer transformer;

    @Context
    private UriInfo uriInfo;

    //The service or policy id to manage version for.
    private final Either<String, String> serviceOrPolicyId;


    /**
     * Creates a new policy version resource for handling policy version requests for the given service or policy
     *
     * @param serviceOrPolicyId The service or policy id to handle policy version requests for. Left is service id,
     *                          right is policy Id
     */
    public PolicyVersionResource(@NotNull final Either<String, String> serviceOrPolicyId) {
        this.serviceOrPolicyId = serviceOrPolicyId;
    }

    /**
     * This will return a list of policy versions for the selected policy. It will return a maximum of count references,
     * it can return fewer references if there are fewer than count entities found. Setting an offset will start listing
     * entities from the given offset. A sort can be specified to allow the resulting list to be sorted in either
     * ascending or descending order. Other params given will be used as search values. Examples:
     * <p/>
     * ?id=a86a7745f9005baa52d380d228a3735a
     * <p/>
     * Returns policy version with id = a86a7745f9005baa52d380d228a3735a
     * <p/>
     * ?active=false&comment=RevisionA&comment=RevisionB
     * <p/>
     * Returns no active policy versions with comments "RevisionA" or "RevisionB"
     * <p/>
     * If any other parameters are given an error will be returned
     *
     * @param offset   The offset to start the listing from. Default is 0
     * @param count    The maximum number of entities to return. Default is 100
     * @param sort     The key to sort the list by. Default is null
     * @param order    The order to sort the list. 'asc' for ascending, 'desc' for descending. Default is ascending
     * @param ids      The id filter
     * @param active   The active filter
     * @param comments The comment filter
     * @return A list of policy versions. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public ItemsList<PolicyVersionMO> listResources(
            @QueryParam("offset") @DefaultValue("0") @NotEmpty final Integer offset,
            @QueryParam("count") @DefaultValue("100") @NotEmpty final Integer count,
            @QueryParam("sort") @ChoiceParam({"id", "version"}) final String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) final String order,
            @QueryParam("id") final List<Goid> ids,
            @QueryParam("active") final Boolean active,
            @QueryParam("comment") final List<String> comments) throws ResourceFactory.ResourceNotFoundException {
        ParameterValidationUtils.validateOffsetCount(offset, count);
        final Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("id", "active", "comment"));

        final CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (ids != null && !ids.isEmpty()) {
            filters.put("goid", (List) ids);
        }
        if (active != null) {
            filters.put("active", (List) Arrays.asList(active));
        }
        if (comments != null && !comments.isEmpty()) {
            //convert empty comments to null
            filters.put("name", new ArrayList(Functions.map(comments, new Functions.Unary<String, String>() {
                @Override
                public String call(String s) {
                    return s == null || s.isEmpty() ? null : s;
                }
            })));
        }
        //create the items list of policy versions
        final List<Item<PolicyVersionMO>> items = Functions.map(policyVersionRestResourceFactory.listPolicyVersions(serviceOrPolicyId, offset, count, sort, ascendingSort, filters.map()), new Functions.Unary<Item<PolicyVersionMO>, PolicyVersionMO>() {
            @Override
            public Item<PolicyVersionMO> call(PolicyVersionMO resource) {
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<PolicyVersionMO>(EntityType.POLICY_VERSION + " list", "List")
                .setContent(items)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    /**
     * Retrieve a policy version by the version number
     *
     * @param versionNumber The version of the policy to return.
     * @return The policy version.
     */
    @GET
    @Path("{versionNumber}")
    public Item<PolicyVersionMO> getResource(@PathParam("versionNumber") @NotEmpty final Long versionNumber) throws ResourceFactory.ResourceNotFoundException {
        final PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getPolicyVersion(serviceOrPolicyId, versionNumber);
        return new ItemBuilder<>(transformer.convertToItem(policyVersion))
                .addLink(getLink(policyVersion))
                .addLinks(getRelatedLinks(policyVersion))
                .build();
    }

    /**
     * Returns the active policy version.
     *
     * @return The active policy version.
     */
    @GET
    @Path("active")
    public Item<PolicyVersionMO> getActiveVersion() throws ResourceFactory.ResourceNotFoundException {
        final PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getActiveVersion(serviceOrPolicyId);
        return new ItemBuilder<>(transformer.convertToItem(policyVersion))
                .addLink(getLink(policyVersion))
                .addLinks(getRelatedLinks(policyVersion))
                .build();
    }

    /**
     * Sets the comment on a specific policy version.
     *
     * @param versionNumber The version of the policy version to set the comment on.
     * @param comment       The comment to set on the policy version. This will override any existing comment
     * @return A reference to the updated policy version
     */
    @PUT
    @Path("{versionNumber}/comment")
    public Response setComment(@PathParam("versionNumber") @NotEmpty final Long versionNumber, @Nullable final String comment) throws ResourceFactory.ResourceNotFoundException {
        policyVersionRestResourceFactory.updateComment(serviceOrPolicyId, versionNumber, comment);
        final PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getPolicyVersion(serviceOrPolicyId, versionNumber);
        return Response.ok(new ItemBuilder<>(transformer.convertToItem(policyVersion))
                .addLink(getLink(policyVersion))
                .addLinks(getRelatedLinks(policyVersion))
                .build()).build();
    }

    /**
     * Setts a comment on the active policy version
     *
     * @param comment The comment to set on the active policy version
     * @return The updated active policy version
     */
    @PUT
    @Path("active/comment")
    public Response setComment(@Nullable final String comment) throws ResourceFactory.ResourceNotFoundException {
        policyVersionRestResourceFactory.updateActiveComment(serviceOrPolicyId, comment);
        final PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getActiveVersion(serviceOrPolicyId);
        return Response.ok(new ItemBuilder<>(transformer.convertToItem(policyVersion))
                .addLink(getLink(policyVersion))
                .addLinks(getRelatedLinks(policyVersion))
                .build()).build();
    }

    /**
     * Activates the specified policy version
     *
     * @param versionNumber The id of the policy version to set active.
     */
    @POST
    @Path("{versionNumber}/activate")
    public void activate(@PathParam("versionNumber") @NotEmpty final Long versionNumber) throws ResourceFactory.ResourceNotFoundException {
        policyVersionRestResourceFactory.activate(serviceOrPolicyId, versionNumber);
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.POLICY_VERSION.toString();
    }

    @NotNull
    @Override
    public String getUrl(@NotNull final PolicyVersionMO policyVersion) {
        return getUrlString(Long.toString(policyVersion.getOrdinal()));
    }

    @NotNull
    @Override
    public String getUrl(@NotNull final EntityHeader policyVersionHeader) {
        throw new UnsupportedOperationException("Cannot get the url of a policy version from its entity header.");
    }

    @NotNull
    @Override
    public Link getLink(@NotNull final PolicyVersionMO policyVersion) {
        return ManagedObjectFactory.createLink("self", getUrl(policyVersion));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final PolicyVersionMO policyVersion) {
        return Arrays.asList(
                ManagedObjectFactory.createLink("list", getUrlString(null)));
    }

    private String getUrlString(@Nullable String versionNumber) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder()
                .path(serviceOrPolicyId.isLeft() ? PublishedServiceResource.class : PolicyResource.class)
                .path(serviceOrPolicyId.isLeft() ? serviceOrPolicyId.left() : serviceOrPolicyId.right())
                .path(VERSIONS_URI);
        if (versionNumber != null) {
            uriBuilder.path(versionNumber);
        }
        return uriBuilder.build().toString();
    }
}
