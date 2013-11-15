package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.RbacRoleResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.api.impl.RemoveAssignmentsContext;

import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import java.util.Map;

/**
 * The rbac role assignment resource
 *
 * @author Victor Kazakov
 */
public class RoleAssignmentsResource {
    private RbacRoleResourceFactory factory;
    private Map<String, String> roleSelectorMap;

    public RoleAssignmentsResource(Map<String, String> roleSelectorMap, RbacRoleResourceFactory factory) {
        this.factory = factory;
        this.roleSelectorMap = roleSelectorMap;
    }

    @PUT
    public void createAssignment(AddAssignmentsContext addAssignmentsContext) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.addAssignments(roleSelectorMap, addAssignmentsContext);
    }

    @DELETE
    public void deleteAssignment( RemoveAssignmentsContext removeAssignmentsContext) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.removeAssignments(roleSelectorMap, removeAssignmentsContext);
    }
}
