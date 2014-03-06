package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyVersionRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ListingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ReadingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyVersionTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;
import org.glassfish.jersey.message.XmlHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;

/**
 * This resource handles policy version operations.
 *
 * @author Victor Kazakov
 */
public class PolicyVersionResource implements ListingResource<PolicyVersionMO>, ReadingResource<PolicyVersionMO>, URLAccessible<PolicyVersionMO> {

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

    @Override
    public ItemsList<PolicyVersionMO> listResources(final ListRequestParameters listRequestParameters) {
        ParameterValidationUtils.validateListRequestParameters(listRequestParameters, policyVersionRestResourceFactory.getSortKeysMap(), policyVersionRestResourceFactory.getFiltersInfo());
        List<Item<PolicyVersionMO>> items = Functions.map(policyVersionRestResourceFactory.listResources(policyId, listRequestParameters.getOffset(), listRequestParameters.getCount(), listRequestParameters.getSort(), listRequestParameters.getOrder(), listRequestParameters.getFiltersMap()), new Functions.Unary<Item<PolicyVersionMO>, PolicyVersionMO>() {
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

    @Override
    public Item<PolicyVersionMO> getResource(String id) throws FindException {
        PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getResource(policyId, id);
        return new ItemBuilder<>(transformer.convertToItem(policyVersion))
                .addLink(getLink(policyVersion))
                .addLinks(getRelatedLinks(policyVersion))
                .build();
    }

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
        if(id != null) {
            uriBuilder.path(id);
        }
        return uriBuilder.build().toString();
    }
}
