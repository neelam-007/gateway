package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.RoleAPIResourceFactory;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.RbacRoleAssignmentMO;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.api.impl.RemoveAssignmentsContext;
import com.l7tech.gateway.rest.SpringBean;

import javax.ws.rs.*;
import java.util.Arrays;
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

    /**
     * This will return a template add assignment context.
     *
     * @return The template add assignments context.
     */
    @GET
    @Path("template/addassignments")
    public Item<AddAssignmentsContext> getAddAssignmentsContextTemplate(){
        AddAssignmentsContext addAssignmentsContext = new AddAssignmentsContext();
        RbacRoleAssignmentMO roleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
        roleAssignmentMO.setEntityType("User or Group");
        roleAssignmentMO.setIdentityName("Name");
        roleAssignmentMO.setProviderId("ProviderID");
        addAssignmentsContext.setAssignments(Arrays.asList(roleAssignmentMO));

        return new ItemBuilder<AddAssignmentsContext>("AddAssignmentsContext Template", "AddAssignmentsContext")
                .setContent(addAssignmentsContext)
                .build();
    }
}
