package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.common.password.Sha512CryptPasswordHasher;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URLEncoder;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class RoleRestEntityResourceTest extends RestEntityTests<Role, RbacRoleMO> {
    private RoleManager roleManager;
    private List<Role> roles = new ArrayList<>();
    private FolderManager folderManager;
    private Folder rootFolder;
    private SecurityZoneManager securityZoneManager;
    private SecurityZone securityZone;
    private GroupManager groupManager;
    private InternalUser role2User;

    @Before
    public void before() throws SaveException, FindException {
        groupManager = provider.getGroupManager();

        roleManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("roleManager", RoleManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        rootFolder = folderManager.findRootFolder();

        securityZone = new SecurityZone();
        securityZone.setName("mySecurityZone");
        securityZoneManager.save(securityZone);

        Role role = new Role();
        role.setName("My Role A");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);
        role.addEntityPermission(OperationType.CREATE, EntityType.POLICY, null);

        roleManager.save(role);
        roles.add(role);

        role = new Role();
        role.setName("My Role B");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);
        role.addFolderPermission(OperationType.READ, EntityType.POLICY, rootFolder, false);

        role2User = new InternalUser();
        role2User.setName("role2User");
        role2User.setLogin("role2User");
        role2User.setHashedPassword(passwordHasher.hashPassword("password".getBytes()));
        userManager.save(role2User, null);
        role.addAssignedUser(role2User);

        roleManager.save(role);
        roles.add(role);

        role = new Role();
        role.setName("My Role C");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);
        role.addAttributePermission(OperationType.DELETE, EntityType.GENERIC, "name", "test");

        roleManager.save(role);
        roles.add(role);

        role = new Role();
        role.setName("My Role D");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);
        role.addEntityFolderAncestryPermission(EntityType.JDBC_CONNECTION, getGoid());

        roleManager.save(role);
        roles.add(role);

        role = new Role();
        role.setName("My Role E");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);
        role.addEntityOtherPermission(EntityType.JMS_CONNECTION, getGoid().toString(), "other operation");

        roleManager.save(role);
        roles.add(role);

        role = new Role();
        role.setName("My Role F");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);
        role.addSecurityZonePermission(OperationType.NONE, EntityType.ANY, securityZone);

        roleManager.save(role);
        roles.add(role);

        role = new Role();
        role.setName("My Role G");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);
        role.addEntityPermission(OperationType.CREATE, EntityType.POLICY, getGoid().toString());

        roleManager.save(role);
        roles.add(role);
    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<Role> all = roleManager.findAll();
        for (final Role role : all) {
            if (role.isUserCreated() || Functions.exists(roles, new Functions.Unary<Boolean, Role>() {
                @Override
                public Boolean call(Role roleCreated) {
                    return Goid.equals(roleCreated.getGoid(), role.getGoid());
                }
            })) {
                roleManager.delete(role.getGoid());
            }
        }
        securityZoneManager.delete(securityZone);
        userManager.delete(role2User);
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(roles, new Functions.Unary<String, Role>() {
            @Override
            public String call(Role role) {
                return role.getId();
            }
        });
    }

    @Override
    public List<RbacRoleMO> getCreatableManagedObjects() {
        List<RbacRoleMO> roles = new ArrayList<>();

        RbacRoleMO role = ManagedObjectFactory.createRbacRoleMO();
        role.setId(getGoid().toString());
        role.setName("My Role Created");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);
        roles.add(role);

        role = ManagedObjectFactory.createRbacRoleMO();
        role.setId(getGoid().toString());
        role.setName("My Role Created 2");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);
        RbacRolePermissionMO rolePermissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
        rolePermissionMO.setEntityType("POLICY");
        rolePermissionMO.setOperation(RbacRolePermissionMO.OperationType.CREATE);
        role.setPermissions(Arrays.asList(rolePermissionMO));
        roles.add(role);

        return roles;
    }

    @Override
    public List<RbacRoleMO> getUpdateableManagedObjects() {
        List<RbacRoleMO> roles = new ArrayList<>();

        Role role = this.roles.get(0);
        RbacRoleMO roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setId(role.getId());
        roleMO.setName("My Role Updated");
        roleMO.setDescription(roleMO.getName() + " Description");
        roleMO.setUserCreated(true);
        roles.add(roleMO);

        //update twice
        roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setId(role.getId());
        roleMO.setName("My Role Updated");
        roleMO.setDescription(roleMO.getName() + " Description");
        roleMO.setUserCreated(true);
        roles.add(roleMO);

        //SSG-8125
        roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setId(role.getId());
        roleMO.setName(role.getName());
        roleMO.setDescription(role.getDescription());
        roleMO.setUserCreated(role.isUserCreated());
        RbacRolePermissionMO rolePermissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
        rolePermissionMO.setEntityType("POLICY");
        rolePermissionMO.setOperation(RbacRolePermissionMO.OperationType.CREATE);
        roleMO.setPermissions(Arrays.asList(rolePermissionMO));
        roles.add(roleMO);

        return roles;
    }

    @Override
    public Map<RbacRoleMO, Functions.BinaryVoid<RbacRoleMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<RbacRoleMO, Functions.BinaryVoid<RbacRoleMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        Role role = this.roles.get(0);
        RbacRoleMO roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setName(role.getName());
        roleMO.setDescription(roleMO.getName() + " Description");
        roleMO.setUserCreated(true);

        builder.put(roleMO, new Functions.BinaryVoid<RbacRoleMO, RestResponse>() {
            @Override
            public void call(RbacRoleMO roleMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8118 - bad role permission entity type
        roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setName("My Role Created with bad entity type");
        roleMO.setDescription(role.getName() + " Description");
        roleMO.setUserCreated(true);
        RbacRolePermissionMO rolePermissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
        rolePermissionMO.setEntityType("Folder");
        rolePermissionMO.setOperation(RbacRolePermissionMO.OperationType.CREATE);
        roleMO.setPermissions(Arrays.asList(rolePermissionMO));

        builder.put(roleMO, new Functions.BinaryVoid<RbacRoleMO, RestResponse>() {
            @Override
            public void call(RbacRoleMO roleMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8118 - bad role permission entity type
        roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setName("My Role Created with bad entity type");
        roleMO.setDescription(role.getName() + " Description");
        roleMO.setUserCreated(true);
        rolePermissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
        rolePermissionMO.setEntityType("POLICY");
        rolePermissionMO.setOperation(RbacRolePermissionMO.OperationType.CREATE);
        RbacRolePredicateMO rolePredicateMO = ManagedObjectFactory.createRbacRolePredicateMO();
        rolePredicateMO.setType(RbacRolePredicateMO.Type.EntityFolderAncestryPredicate);
        rolePredicateMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("entityType", "Folder")
                .put("entityId", getGoid().toString())
                .map());
        rolePermissionMO.setScope(Arrays.asList(rolePredicateMO));
        roleMO.setPermissions(Arrays.asList(rolePermissionMO));

        builder.put(roleMO, new Functions.BinaryVoid<RbacRoleMO, RestResponse>() {
            @Override
            public void call(RbacRoleMO roleMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<RbacRoleMO, Functions.BinaryVoid<RbacRoleMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<RbacRoleMO, Functions.BinaryVoid<RbacRoleMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        Role role = this.roles.get(0);
        RbacRoleMO roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setId(role.getId());
        roleMO.setName(this.roles.get(1).getName());
        roleMO.setDescription(roleMO.getName() + " Description");
        roleMO.setUserCreated(true);

        builder.put(roleMO, new Functions.BinaryVoid<RbacRoleMO, RestResponse>() {
            @Override
            public void call(RbacRoleMO roleMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8118 - bad role permission entity type
        roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setId(role.getId());
        roleMO.setName(role.getName());
        roleMO.setDescription(role.getDescription());
        roleMO.setUserCreated(true);
        RbacRolePermissionMO rolePermissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
        rolePermissionMO.setEntityType("Folder");
        rolePermissionMO.setOperation(RbacRolePermissionMO.OperationType.CREATE);
        roleMO.setPermissions(Arrays.asList(rolePermissionMO));

        builder.put(roleMO, new Functions.BinaryVoid<RbacRoleMO, RestResponse>() {
            @Override
            public void call(RbacRoleMO roleMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8118 - bad role permission entity type
        roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setId(role.getId());
        roleMO.setName(role.getName());
        roleMO.setDescription(role.getDescription());
        roleMO.setUserCreated(true);
        rolePermissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
        rolePermissionMO.setEntityType("POLICY");
        rolePermissionMO.setOperation(RbacRolePermissionMO.OperationType.CREATE);
        RbacRolePredicateMO rolePredicateMO = ManagedObjectFactory.createRbacRolePredicateMO();
        rolePredicateMO.setType(RbacRolePredicateMO.Type.EntityFolderAncestryPredicate);
        rolePredicateMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("entityType", "Folder")
                .put("entityId", getGoid().toString())
                .map());
        rolePermissionMO.setScope(Arrays.asList(rolePredicateMO));
        roleMO.setPermissions(Arrays.asList(rolePermissionMO));

        builder.put(roleMO, new Functions.BinaryVoid<RbacRoleMO, RestResponse>() {
            @Override
            public void call(RbacRoleMO roleMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(roles, new Functions.Unary<String, Role>() {
            @Override
            public String call(Role role) {
                return role.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "roles";
    }

    @Override
    public String getType() {
        return EntityType.RBAC_ROLE.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        Role entity = roleManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        Role entity = roleManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, RbacRoleMO managedObject) throws FindException {
        Role entity = roleManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getDescription(), managedObject.getDescription());
            Assert.assertEquals(entity.isUserCreated().booleanValue(), managedObject.isUserCreated());
            Assert.assertEquals(entity.getPermissions().size(), managedObject.getPermissions().size());
            for (final Permission permission : entity.getPermissions()) {
                //todo: need a way to confirm that newly created/updated role permissions are also valid.
                if (managedObject.getPermissions().get(0).getId() != null) {
                    RbacRolePermissionMO rolePermissionMO = Functions.grepFirst(managedObject.getPermissions(), new Functions.Unary<Boolean, RbacRolePermissionMO>() {
                        @Override
                        public Boolean call(RbacRolePermissionMO rbacRolePermissionMO) {
                            return permission.getId().equals(rbacRolePermissionMO.getId());
                        }
                    });
                    Assert.assertNotNull("missing permission with id: " + permission.getId(), rolePermissionMO);
                    Assert.assertEquals(permission.getOtherOperationName(), rolePermissionMO.getOtherOperationName());
                    Assert.assertEquals(permission.getEntityType().name(), rolePermissionMO.getEntityType());
                    Assert.assertEquals(permission.getOperation().name(), rolePermissionMO.getOperation().name());
                    Assert.assertEquals(permission.getScope().size(), rolePermissionMO.getScope().size());
                    for (final ScopePredicate scope : permission.getScope()) {
                        RbacRolePredicateMO rolePredicateMO = Functions.grepFirst(rolePermissionMO.getScope(), new Functions.Unary<Boolean, RbacRolePredicateMO>() {
                            @Override
                            public Boolean call(RbacRolePredicateMO rbacRolePredicateMO) {
                                return scope.getId().equals(rbacRolePredicateMO.getId());
                            }
                        });
                        Assert.assertNotNull("missing predicate with id: " + scope.getId(), rolePredicateMO);
                        if (scope instanceof AttributePredicate) {
                            AttributePredicate attributePredicate = (AttributePredicate) scope;
                            Assert.assertEquals(RbacRolePredicateMO.Type.AttributePredicate, rolePredicateMO.getType());
                            Assert.assertEquals(attributePredicate.getAttribute(), rolePredicateMO.getProperties().get("attribute"));
                            Assert.assertEquals(attributePredicate.getMode(), rolePredicateMO.getProperties().get("mode"));
                            Assert.assertEquals(attributePredicate.getValue(), rolePredicateMO.getProperties().get("value"));
                        } else if (scope instanceof EntityFolderAncestryPredicate) {
                            EntityFolderAncestryPredicate entityFolderAncestryPredicate = (EntityFolderAncestryPredicate) scope;
                            Assert.assertEquals(RbacRolePredicateMO.Type.EntityFolderAncestryPredicate, rolePredicateMO.getType());
                            Assert.assertEquals(entityFolderAncestryPredicate.getEntityType().name(), rolePredicateMO.getProperties().get("entityType"));
                            Assert.assertEquals(entityFolderAncestryPredicate.getEntityId(), rolePredicateMO.getProperties().get("entityId"));
                        } else if (scope instanceof FolderPredicate) {
                            FolderPredicate folderPredicate = (FolderPredicate) scope;
                            Assert.assertEquals(RbacRolePredicateMO.Type.FolderPredicate, rolePredicateMO.getType());
                            Assert.assertEquals(folderPredicate.getFolder().getId(), rolePredicateMO.getProperties().get("folderId"));
                            Assert.assertEquals(Boolean.toString(folderPredicate.isTransitive()), rolePredicateMO.getProperties().get("transitive"));
                        } else if (scope instanceof ObjectIdentityPredicate) {
                            ObjectIdentityPredicate objectIdentityPredicate = (ObjectIdentityPredicate) scope;
                            Assert.assertEquals(RbacRolePredicateMO.Type.ObjectIdentityPredicate, rolePredicateMO.getType());
                            Assert.assertEquals(objectIdentityPredicate.getTargetEntityId(), rolePredicateMO.getProperties().get("entityId"));
                        } else if (scope instanceof SecurityZonePredicate) {
                            SecurityZonePredicate securityZonePredicate = (SecurityZonePredicate) scope;
                            Assert.assertEquals(RbacRolePredicateMO.Type.SecurityZonePredicate, rolePredicateMO.getType());
                            Assert.assertEquals(securityZonePredicate.getRequiredZone().getId(), rolePredicateMO.getProperties().get("securityZoneId"));
                        }
                    }
                }
            }
            Assert.assertEquals(entity.getRoleAssignments().size(), managedObject.getAssignments().size());
            for (final RoleAssignment roleAssignment : entity.getRoleAssignments()) {
                RbacRoleAssignmentMO roleAssignmentMO = Functions.grepFirst(managedObject.getAssignments(), new Functions.Unary<Boolean, RbacRoleAssignmentMO>() {
                    @Override
                    public Boolean call(RbacRoleAssignmentMO rbacRolePermissionMO) {
                        return roleAssignment.getId().equals(rbacRolePermissionMO.getId());
                    }
                });
                Assert.assertNotNull("missing permission with id: " + roleAssignment.getId(), roleAssignmentMO);
                Assert.assertEquals(roleAssignment.getEntityType(), roleAssignmentMO.getEntityType());
                Assert.assertEquals(roleAssignment.getIdentityId(), roleAssignmentMO.getIdentityId());
                Assert.assertEquals(roleAssignment.getProviderId().toString(), roleAssignmentMO.getProviderId());
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(roleManager.findAll(), new Functions.Unary<String, Role>() {
                    @Override
                    public String call(Role role) {
                        return role.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(roles.get(0).getName()), Arrays.asList(roles.get(0).getId()))
                .put("name=" + URLEncoder.encode(roles.get(0).getName()) + "&name=" + URLEncoder.encode(roles.get(1).getName()), Functions.map(roles.subList(0, 2), new Functions.Unary<String, Role>() {
                    @Override
                    public String call(Role role) {
                        return role.getId();
                    }
                }))
                .put("name=badName", Collections.<String>emptyList())
                .map();
    }

    private final PasswordHasher passwordHasher = new Sha512CryptPasswordHasher();

    @Test
    public void addRoleAssignment() throws Exception {
        InternalUser user = new InternalUser();
        user.setName("rbacUser");
        user.setLogin("rbacUser");
        user.setHashedPassword(passwordHasher.hashPassword("password".getBytes()));
        userManager.save(user, null);

        try {
            AddAssignmentsContext addAssignmentsContext = new AddAssignmentsContext();
            RbacRoleAssignmentMO rbacRoleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
            rbacRoleAssignmentMO.setIdentityId(user.getId());
            rbacRoleAssignmentMO.setProviderId(user.getProviderId().toString());
            rbacRoleAssignmentMO.setEntityType("User");
            addAssignmentsContext.setAssignments(Arrays.asList(rbacRoleAssignmentMO));

            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + roles.get(0).getId() + "/assignments", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(addAssignmentsContext));

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals(204, response.getStatus());

            Role role = roleManager.findByPrimaryKey(roles.get(0).getGoid());
            Assert.assertNotNull(role);
            Assert.assertNotNull(role.getRoleAssignments());
            Assert.assertEquals(1, role.getRoleAssignments().size());
            RoleAssignment roleAssignment = role.getRoleAssignments().iterator().next();
            Assert.assertNotNull(roleAssignment);
            Assert.assertEquals("User", roleAssignment.getEntityType());
            Assert.assertEquals(user.getId(), roleAssignment.getIdentityId());
            Assert.assertEquals(user.getProviderId(), roleAssignment.getProviderId());
        } finally {
            userManager.delete(user);
        }
    }

    @Test
    public void addTwoRoleAssignments() throws Exception {
        InternalUser user = new InternalUser();
        user.setName("rbacUser");
        user.setLogin("rbacUser");
        user.setHashedPassword(passwordHasher.hashPassword("password".getBytes()));
        userManager.save(user, null);
        //the second user
        final InternalUser user2 = new InternalUser();
        user2.setName("rbacUser2");
        user2.setLogin("rbacUser2");
        user2.setHashedPassword(passwordHasher.hashPassword("password".getBytes()));
        userManager.save(user2, null);
        try {

            AddAssignmentsContext addAssignmentsContext = new AddAssignmentsContext();
            RbacRoleAssignmentMO rbacRoleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
            rbacRoleAssignmentMO.setIdentityId(user.getId());
            rbacRoleAssignmentMO.setProviderId(user.getProviderId().toString());
            rbacRoleAssignmentMO.setEntityType("User");
            addAssignmentsContext.setAssignments(Arrays.asList(rbacRoleAssignmentMO));

            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + roles.get(0).getId() + "/assignments", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(addAssignmentsContext));

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals(204, response.getStatus());

            Role role = roleManager.findByPrimaryKey(roles.get(0).getGoid());
            Assert.assertNotNull(role);
            Assert.assertNotNull(role.getRoleAssignments());
            Assert.assertEquals(1, role.getRoleAssignments().size());
            RoleAssignment roleAssignment = role.getRoleAssignments().iterator().next();
            Assert.assertNotNull(roleAssignment);
            Assert.assertEquals("User", roleAssignment.getEntityType());
            Assert.assertEquals(user.getId(), roleAssignment.getIdentityId());
            Assert.assertEquals(user.getProviderId(), roleAssignment.getProviderId());

            addAssignmentsContext = new AddAssignmentsContext();
            rbacRoleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
            rbacRoleAssignmentMO.setIdentityId(user2.getId());
            rbacRoleAssignmentMO.setProviderId(user2.getProviderId().toString());
            rbacRoleAssignmentMO.setEntityType("User");
            addAssignmentsContext.setAssignments(Arrays.asList(rbacRoleAssignmentMO));

            response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + roles.get(0).getId() + "/assignments", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(addAssignmentsContext));

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals(204, response.getStatus());

            role = roleManager.findByPrimaryKey(roles.get(0).getGoid());
            Assert.assertNotNull(role);
            Assert.assertNotNull(role.getRoleAssignments());
            Assert.assertEquals(2, role.getRoleAssignments().size());
            roleAssignment = Functions.grepFirst(role.getRoleAssignments(), new Functions.Unary<Boolean, RoleAssignment>() {
                @Override
                public Boolean call(RoleAssignment roleAssignment) {
                    return user2.getId().equals(roleAssignment.getIdentityId());
                }
            });
            Assert.assertNotNull(roleAssignment);
            Assert.assertEquals("User", roleAssignment.getEntityType());
            Assert.assertEquals(user2.getId(), roleAssignment.getIdentityId());
            Assert.assertEquals(user2.getProviderId(), roleAssignment.getProviderId());
        } finally {
            userManager.delete(user);
            userManager.delete(user2);
        }
    }

    @Test
    public void addGroupRoleAssignment() throws Exception {
        InternalGroup group = new InternalGroup();
        group.setName("rbacGroup");
        groupManager.save(group, null);
        try {
            AddAssignmentsContext addAssignmentsContext = new AddAssignmentsContext();
            RbacRoleAssignmentMO rbacRoleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
            rbacRoleAssignmentMO.setIdentityId(group.getId());
            rbacRoleAssignmentMO.setProviderId(group.getProviderId().toString());
            rbacRoleAssignmentMO.setEntityType("Group");
            addAssignmentsContext.setAssignments(Arrays.asList(rbacRoleAssignmentMO));

            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + roles.get(0).getId() + "/assignments", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(addAssignmentsContext));

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals(204, response.getStatus());

            Role role = roleManager.findByPrimaryKey(roles.get(0).getGoid());
            Assert.assertNotNull(role);
            Assert.assertNotNull(role.getRoleAssignments());
            Assert.assertEquals(1, role.getRoleAssignments().size());
            RoleAssignment roleAssignment = role.getRoleAssignments().iterator().next();
            Assert.assertNotNull(roleAssignment);
            Assert.assertEquals("Group", roleAssignment.getEntityType());
            Assert.assertEquals(group.getId(), roleAssignment.getIdentityId());
            Assert.assertEquals(group.getProviderId(), roleAssignment.getProviderId());
        } finally {
            groupManager.delete(group);
        }
    }

    @Test
    public void addNonExistingRoleAssignment() throws Exception {
        AddAssignmentsContext addAssignmentsContext = new AddAssignmentsContext();
        RbacRoleAssignmentMO rbacRoleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
        rbacRoleAssignmentMO.setIdentityId(getGoid().toString());
        rbacRoleAssignmentMO.setProviderId(new Goid(0,-2).toString());
        rbacRoleAssignmentMO.setEntityType("User");
        addAssignmentsContext.setAssignments(Arrays.asList(rbacRoleAssignmentMO));

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + roles.get(0).getId() + "/assignments", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(addAssignmentsContext));

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void removeRoleAssignment() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + roles.get(1).getId() + "/assignments", "id=" + roles.get(1).getRoleAssignments().iterator().next().getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), null);

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(204, response.getStatus());

        Role role = roleManager.findByPrimaryKey(roles.get(1).getGoid());
        Assert.assertNotNull(role);
        Assert.assertNotNull(role.getRoleAssignments());
        Assert.assertTrue(role.getRoleAssignments().isEmpty());
    }

    @Test
    public void removeNotExistingRoleAssignment() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + roles.get(1).getId() + "/assignments", "id=" + getGoid().toString(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), null);

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(400, response.getStatus());
    }
}
