package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.PolicyVersionMO;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.Reference;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.References;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.util.CollectionUtils;
import org.glassfish.jersey.message.XmlHeader;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * This resource handles policy version operations.
 *
 * @author Victor Kazakov
 */
public class PolicyVersionResource {

    /**
     * This is used to create resource uri's
     */
    @Context
    private UriInfo uriInfo;

    @SpringBean
    private PolicyVersionManager policyVersionManager;

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
     * entities from the given offset. Filters can be used to filter out entities based on thier properties. Currently
     * only equality filters are possible.
     *
     * @param offset  The offset from the start of the list to start listing from
     * @param count   The total number of entities to return. The returned list can be shorter is there are not enough
     *                entities
     * @param filters This is a collection of filters to apply to the list.
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    //This xml header allows the list to be explorable when viewed in a browser
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response listResources(@QueryParam("offset") @DefaultValue("0") @Min(0) int offset, @QueryParam("count") @DefaultValue("100") @Min(1) @Max(500) int count, @QueryParam("filters") String filters) throws FindException {
        //TODO: implement filtering.
        //gets the list of resource ids
        final List<PolicyVersion> policyVersions = CollectionUtils.safeSubList(policyVersionManager.findAllForPolicy(Goid.parseGoid(policyId)), offset, offset + count);
        //Create the Reference list.
        List<Reference> resourceList = new ArrayList<>(policyVersions.size());
        for (PolicyVersion policyVersion : policyVersions) {
            UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(policyVersion.getId());
            final URI uri = ub.build();
            resourceList.add(new Reference(uri.toString(), uri.toString()));
        }

        return Response.ok(new References(resourceList)).build();
    }

    /**
     * This implements the GET method to retrieve an entity by a given id.
     *
     * @param id The identity of the entity to select
     * @return The selected entity.
     */
    @GET
    @Path("{id}")
    public Response getResource(@PathParam("id") String id) throws FindException {
        PolicyVersion policyVersion = policyVersionManager.findByPrimaryKey(Goid.parseGoid(policyId), Goid.parseGoid(id));
        return Response.ok(buildMO(policyVersion)).build();
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
        PolicyVersion policyVersion = policyVersionManager.findByPrimaryKey(Goid.parseGoid(policyId), Goid.parseGoid(id));
        policyVersion.setName(comment);
        policyVersionManager.update(policyVersion);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path("../");
        final URI uri = ub.build().normalize();
        return Response.ok(new Reference(uri.toString(), uri.toString())).build();
    }

    /**
     * Builds a policy version MO
     *
     * @param policyVersion The plicy version to build the MO from
     * @return The policy Version MO
     */
    private PolicyVersionMO buildMO(PolicyVersion policyVersion) {
        PolicyVersionMO policyVersionMO = new PolicyVersionMO();
        policyVersionMO.setActive(policyVersion.isActive());
        policyVersionMO.setComment(policyVersion.getName());
        policyVersionMO.setId(policyVersion.getId());
        policyVersionMO.setPolicyId(policyVersion.getPolicyGoid().toString());
        policyVersionMO.setTime(policyVersion.getTime());
        policyVersionMO.setVersion(policyVersion.getOrdinal());
        policyVersionMO.setXml(policyVersion.getXml());
        return policyVersionMO;
    }
}
