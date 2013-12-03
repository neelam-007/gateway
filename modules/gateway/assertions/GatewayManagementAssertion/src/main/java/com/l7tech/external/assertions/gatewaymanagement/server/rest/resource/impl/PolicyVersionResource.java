package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.PolicyVersionMO;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyVersionRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ListingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ReadingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResourceUtils;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;
import org.glassfish.jersey.message.XmlHeader;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * This resource handles policy version operations.
 *
 * @author Victor Kazakov
 */
public class PolicyVersionResource implements ListingResource, ReadingResource {

    @SpringBean
    private PolicyVersionRestResourceFactory policyVersionRestResourceFactory;

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
    public Response listResources(final int offset, final int count, final String sort, final String order) {
        final String sortKey = policyVersionRestResourceFactory.getSortKey(sort);
        if (sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Reference> references = Functions.map(policyVersionRestResourceFactory.listResources(policyId, offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(policyVersionRestResourceFactory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Reference, PolicyVersionMO>() {
            @Override
            public Reference call(PolicyVersionMO resource) {
                return toReference(resource);
            }
        });
        return Response.ok(ManagedObjectFactory.createReferences(references)).build();
    }

    private Reference toReference(PolicyVersionMO resource) {
        return ManagedObjectFactory.createReference(RestEntityResourceUtils.createURI(uriInfo.getAbsolutePath(), resource.getId()), resource.getId(), EntityType.POLICY_VERSION.name(), "Version: " + resource.getVersion());
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
    public Response setComment(@PathParam("id") String id, String comment) throws FindException, UpdateException {
        policyVersionRestResourceFactory.updateComment(policyId, id, comment);
        return Response.ok(toReference(policyVersionRestResourceFactory.getResource(policyId, id))).build();
    }
}
