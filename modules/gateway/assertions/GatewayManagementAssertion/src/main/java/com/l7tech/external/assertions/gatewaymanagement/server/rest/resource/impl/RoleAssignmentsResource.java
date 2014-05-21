package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.RoleAPIResourceFactory;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.api.impl.RemoveAssignmentsContext;
import com.l7tech.gateway.rest.SpringBean;

import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import java.util.List;

/**
 * The rbac role assignment resource
 */
public class RoleAssignmentsResource {
    @SpringBean
    private RoleAPIResourceFactory factory;

    private String roleId;

    public RoleAssignmentsResource(String roleId) {
        this.roleId = roleId;
    }

    /**
     * Adds role assignments.
     *
     * @param addAssignmentsContext The role assignments to add.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    public void createAssignment(AddAssignmentsContext addAssignmentsContext) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.addAssignments(roleId, addAssignmentsContext);
    }

    /**
     * Removes role assignments
     *
     * @param assignmentIds The assignments to remove
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @DELETE
    public void deleteAssignment(@QueryParam("id") List<String> assignmentIds) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        RemoveAssignmentsContext removeAssignmentsContext = new RemoveAssignmentsContext();
        removeAssignmentsContext.setAssignmentIds(assignmentIds);
        factory.removeAssignments(roleId, removeAssignmentsContext);
    }
}
