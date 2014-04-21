package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.net.URLEncoder;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class RoleRestEntityResourceTest extends RestEntityTests<Role, RbacRoleMO> {
    private RoleManager roleManager;
    private List<Role> roles = new ArrayList<>();

    @Before
    public void before() throws SaveException {
        roleManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("roleManager", RoleManager.class);
        //Create the active connectors

        Role role = new Role();
        role.setName("My Role A");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);

        roleManager.save(role);
        roles.add(role);

        role = new Role();
        role.setName("My Role B");
        role.setDescription(role.getName() + " Description");
        role.setUserCreated(true);

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
        rolePredicateMO.setProperties(CollectionUtils.MapBuilder.<String,String>builder()
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
        rolePredicateMO.setProperties(CollectionUtils.MapBuilder.<String,String>builder()
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
}
