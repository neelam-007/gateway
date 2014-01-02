package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.RbacRoleResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.api.impl.RemoveAssignmentsContext;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * This was created: 11/18/13 as 11:58 AM
 *
 * @author Victor Kazakov
 */
@Component
public class RoleRestResourceFactory extends WsmanBaseResourceFactory<RbacRoleMO, RbacRoleResourceFactory> {

    public RoleRestResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name").map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("userCreated", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("userCreated", RestResourceFactoryUtils.booleanConvert))
                        .map());
    }

    @NotNull
    @Override
    public EntityType getEntityType(){
        return EntityType.RBAC_ROLE;
    }

    @Override
    @Inject
    public void setFactory(RbacRoleResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public RbacRoleMO getResourceTemplate() {
        RbacRoleMO roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setName("TemplateRole");
        roleMO.setDescription("This is an example description");

        RbacRolePermissionMO permissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
        permissionMO.setEntityType("ExampleEntity");
        permissionMO.setOperation(RbacRolePermissionMO.OperationType.CREATE);

        RbacRolePredicateMO rolePredicateMO = ManagedObjectFactory.createRbacRolePredicateMO();
        rolePredicateMO.setType(RbacRolePredicateMO.Type.AttributePredicate);
        rolePredicateMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("ExampleAttributeName", "ExampleAttributeValue").map());
        permissionMO.setScope(Arrays.asList(rolePredicateMO));

        roleMO.setPermissions(Arrays.asList(permissionMO));

        RbacRoleAssignmentMO roleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
        roleAssignmentMO.setEntityType("User or Group");
        roleAssignmentMO.setIdentityName("Name");
        roleAssignmentMO.setProviderId("ProviderID");

        roleMO.setAssignments(Arrays.asList(roleAssignmentMO));
        return roleMO;
    }

    public void addAssignments(String roleId, AddAssignmentsContext addAssignmentsContext) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.addAssignments(buildSelectorMap(roleId), addAssignmentsContext);
    }

    public void removeAssignments(String roleId, RemoveAssignmentsContext removeAssignmentsContext) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        factory.removeAssignments(buildSelectorMap(roleId), removeAssignmentsContext);
    }
}
