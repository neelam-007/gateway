package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.RoleAPIResourceFactory;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.api.impl.RemoveAssignmentsContext;
import com.l7tech.gateway.rest.SpringBean;

import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;

/**
 * The rbac role assignment resource
 *
 * @author Victor Kazakov
 */
public class RoleAssignmentsResource {
    @SpringBean
    private RoleAPIResourceFactory factory;

    private String roleId;

    public RoleAssignmentsResource(String roleId) {
        this.roleId = roleId;
    }

    @PUT
    public void createAssignment(AddAssignmentsContext addAssignmentsContext) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.addAssignments(roleId, addAssignmentsContext);
    }

    @DELETE
    public void deleteAssignment( RemoveAssignmentsContext removeAssignmentsContext) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.removeAssignments(roleId, removeAssignmentsContext);
    }
}
