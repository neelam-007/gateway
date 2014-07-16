package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyVersionRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.*;
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
     * <p>Returns a list of policy versions. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort     Key to sort the list by
     * @param order    Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *                 ascending if not specified
     * @param ids      Id filter
     * @param active   Active filter
     * @param comments Comment filter
     * @return A list of policy versions. If the list is empty then no policy versions were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public ItemsList<PolicyVersionMO> listVersions(
            @QueryParam("sort") @ChoiceParam({"id", "version"}) final String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) final String order,
            @QueryParam("id") final List<Goid> ids,
            @QueryParam("active") final Boolean active,
            @QueryParam("comment") final List<String> comments) throws ResourceFactory.ResourceNotFoundException {
        final Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("id", "active", "comment"));

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
        return RestEntityResourceUtils.createItemsList(
                policyVersionRestResourceFactory.listPolicyVersions(serviceOrPolicyId, sort, ascendingSort, filters.map()),
                transformer,
                this,
                uriInfo.getRequestUri().toString());
    }

    /**
     * Retrieve a policy version by the version number
     *
     * @param versionNumber Version of the policy to return.
     * @return The policy version.
     */
    @GET
    @Path("{versionNumber}")
    public Item<PolicyVersionMO> get(@PathParam("versionNumber") @NotEmpty final Long versionNumber) throws ResourceFactory.ResourceNotFoundException {
        final PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getPolicyVersion(serviceOrPolicyId, versionNumber);
        return RestEntityResourceUtils.createGetResponseItem(policyVersion, transformer, this);
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
        return RestEntityResourceUtils.createGetResponseItem(policyVersion, transformer, this);
    }

    /**
     * Sets the comment on a specific policy version.
     *
     * @param versionNumber Version of the policy version to set the comment on.
     * @param comment       Comment to set on the policy version. This will override any existing comment
     * @return A reference to the updated policy version
     */
    @PUT
    @Path("{versionNumber}/comment")
    public Item<PolicyVersionMO> setComment(@PathParam("versionNumber") @NotEmpty final Long versionNumber, @Nullable final String comment) throws ResourceFactory.ResourceNotFoundException {
        policyVersionRestResourceFactory.updateComment(serviceOrPolicyId, versionNumber, comment);
        final PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getPolicyVersion(serviceOrPolicyId, versionNumber);
        return RestEntityResourceUtils.createGetResponseItem(policyVersion, transformer, this);
    }

    /**
     * Sets a comment on the active policy version
     *
     * @param comment Comment to set on the active policy version
     * @return The updated active policy version
     */
    @PUT
    @Path("active/comment")
    public Item<PolicyVersionMO> setActiveVersionComment(@Nullable final String comment) throws ResourceFactory.ResourceNotFoundException {
        policyVersionRestResourceFactory.updateActiveComment(serviceOrPolicyId, comment);
        final PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getActiveVersion(serviceOrPolicyId);
        return RestEntityResourceUtils.createGetResponseItem(policyVersion, transformer, this);
    }

    /**
     * Activates the specified policy version
     *
     * @param versionNumber Version of the policy version to set active.
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
        return ManagedObjectFactory.createLink(Link.LINK_REL_SELF, getUrl(policyVersion));
    }

    @NotNull
    @Override
    public List<Link> getRelatedLinks(@Nullable final PolicyVersionMO policyVersion) {
        ArrayList<Link> links = new ArrayList<>();
        links.add(ManagedObjectFactory.createLink(Link.LINK_REL_LIST, getUrlString(null)));
        links.add(ManagedObjectFactory.createLink("active", getUrlString("active")));
        if (policyVersion != null) {
            links.add(ManagedObjectFactory.createLink(serviceOrPolicyId.isLeft() ? "service" : "policy", uriInfo.getBaseUriBuilder()
                    .path(serviceOrPolicyId.isLeft() ? PublishedServiceResource.class : PolicyResource.class)
                    .path(serviceOrPolicyId.isLeft() ? serviceOrPolicyId.left() : serviceOrPolicyId.right()).build().toString()));
        }
        return links;
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
