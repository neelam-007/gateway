package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.PolicyVersionMO;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.Reference;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyVersionRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ListingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ReadingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResourceUtils;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import org.glassfish.jersey.message.XmlHeader;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;

/**
 * This resource handles policy version operations.
 *
 * @author Victor Kazakov
 */
public class PolicyVersionResource implements ListingResource, ReadingResource {

    @SpringBean
    private PolicyVersionRestResourceFactory policyVersionRestResourceFactory;

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
    public Response listResources(UriInfo uriInfo, final int offset, final int count, final String sort, final String order) {
        final String sortKey = policyVersionRestResourceFactory.getSortKey(sort);
        if (sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        return RestEntityResourceUtils.createReferenceListResponse(uriInfo.getAbsolutePath(), policyVersionRestResourceFactory.listResources(policyId, offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(policyVersionRestResourceFactory.getFiltersInfo(), uriInfo.getQueryParameters())));
    }

    @Override
    public Response getResource(String id) throws FindException {
        PolicyVersionMO policyVersion = policyVersionRestResourceFactory.getResource(policyId, id);
        return Response.ok(policyVersion).build();
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
    public Response setComment(@Context UriInfo uriInfo, @PathParam("id") String id, String comment) throws FindException, UpdateException {
        policyVersionRestResourceFactory.updateComment(policyId, id, comment);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path("../");
        final URI uri = ub.build().normalize();
        return Response.ok(new Reference(uri.toString(), uri.toString())).build();
    }
}
