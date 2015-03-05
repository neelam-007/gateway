package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.RbacRoleResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.RbacRoleMO;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.api.impl.RemoveAssignmentsContext;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This was created: 11/18/13 as 11:58 AM
 *
 * @author Victor Kazakov
 */
@Component
public class RoleAPIResourceFactory extends WsmanBaseResourceFactory<RbacRoleMO, RbacRoleResourceFactory> {

    public RoleAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.RBAC_ROLE.toString();
    }

    @Override
    @Inject
    @Named("rbacRoleResourceFactory")
    public void setFactory(RbacRoleResourceFactory factory) {
        super.factory = factory;
    }

    public void addAssignments(String roleId, AddAssignmentsContext addAssignmentsContext) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.addAssignments(buildSelectorMap(roleId), addAssignmentsContext);
    }

    public void removeAssignments(String roleId, RemoveAssignmentsContext removeAssignmentsContext) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.removeAssignments(buildSelectorMap(roleId), removeAssignmentsContext);
    }
}
