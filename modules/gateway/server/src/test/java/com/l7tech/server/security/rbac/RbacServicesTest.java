package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.EntityProtectionInfo;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.server.*;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RbacServicesTest {

    //- PUBLIC

    @Test
    public void testPermittedForEntity() throws Exception {
        final RbacServices rbacServices = new RbacServicesImpl(roleManager , entityFinder, null );

        final PublishedService service = new PublishedService();
        service.setGoid(new Goid(0,123));
        service.setName( "Test Service" );

        final boolean permitted1 = rbacServices.isPermittedForEntity( newUserBean("none"), service, OperationType.READ, null );
        assertFalse("No permissions", permitted1);

        final boolean permitted2 = rbacServices.isPermittedForEntity( newUserBean("any"), service, OperationType.READ, null );
        assertTrue("Any permission", permitted2);

        final boolean permitted3 = rbacServices.isPermittedForEntity( newUserBean("some"), service, OperationType.READ, null );
        assertTrue("Some permission", permitted3);

        final boolean permitted4 = rbacServices.isPermittedForEntity( newUserBean("some-other"), service, OperationType.READ, null );
        assertFalse("Some other entity permission", permitted4);

        final boolean permitted5 = rbacServices.isPermittedForSomeEntityOfType( newUserBean("other-ent"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other entity type permission", permitted5);

        final boolean permitted6 = rbacServices.isPermittedForSomeEntityOfType( newUserBean("other-perm"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other operation permission", permitted6);
    }

    @Test
    public void testPermittedForAnyEntity() throws Exception {
        final RbacServices rbacServices = new RbacServicesImpl(roleManager , entityFinder, null );

        final boolean permitted1 = rbacServices.isPermittedForAnyEntityOfType( newUserBean("none"), OperationType.READ, EntityType.SERVICE );
        assertFalse("No permissions", permitted1);

        final boolean permitted2 = rbacServices.isPermittedForAnyEntityOfType( newUserBean("any"), OperationType.READ, EntityType.SERVICE );
        assertTrue("Any permission", permitted2);

        final boolean permitted3 = rbacServices.isPermittedForAnyEntityOfType( newUserBean("some"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Some permission", permitted3);

        final boolean permitted4 = rbacServices.isPermittedForSomeEntityOfType( newUserBean("other-ent"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other entity permission", permitted4);

        final boolean permitted5 = rbacServices.isPermittedForSomeEntityOfType( newUserBean("other-perm"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other operation permission", permitted5);
    }

    @Test
    public void testPermittedForSomeEntity() throws Exception {
        final RbacServices rbacServices = new RbacServicesImpl(roleManager , entityFinder, null );

        final boolean permitted1 = rbacServices.isPermittedForSomeEntityOfType( newUserBean("none"), OperationType.READ, EntityType.SERVICE );
        assertFalse("No permissions", permitted1);

        final boolean permitted2 = rbacServices.isPermittedForSomeEntityOfType( newUserBean("any"), OperationType.READ, EntityType.SERVICE );
        assertTrue("Any permission", permitted2);

        final boolean permitted3 = rbacServices.isPermittedForSomeEntityOfType( newUserBean("some"), OperationType.READ, EntityType.SERVICE );
        assertTrue("Some permission", permitted3);

        final boolean permitted4 = rbacServices.isPermittedForSomeEntityOfType( newUserBean("other-ent"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other entity permission", permitted4);

        final boolean permitted5 = rbacServices.isPermittedForSomeEntityOfType( newUserBean("other-perm"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other operation permission", permitted5);
    }

    @Test
    public void testPermittedForEntityWithProtectedEntityTracker() throws Exception {
        final ServerConfig config = new ServerConfigStub();
        config.putProperty(ServerConfigParams.PARAM_PROTECTED_ENTITY_TRACKER_ENABLE, String.valueOf(true));
        final ProtectedEntityTracker protectedEntityTracker = new ProtectedEntityTracker(config);
        ConfigFactory.clearCachedConfig();
        Assert.assertNotNull(protectedEntityTracker);
        Assert.assertTrue(protectedEntityTracker.isEnabled());
        final RbacServices rbacServices = new RbacServicesImpl(roleManager , entityFinder, protectedEntityTracker);

        // create our test admin user
        final User adminUser = newUserBean("admin");

        // create sample entities
        final Collection<Entity> testEntities = Collections.unmodifiableCollection(
                CollectionUtils.<Entity>list(
                        new PublishedService() {{
                            setGoid(new Goid(0, 1));
                            setName("Test Service");
                        }},
                        new ServerModuleFile() {{
                            setGoid(new Goid(0, 2));
                            setName("Test SMF");
                        }},
                        new SecurePassword() {{
                            setGoid(new Goid(0, 3));
                            setName("Test Password");
                        }},
                        new EncapsulatedAssertionConfig() {{
                            setGoid(new Goid(0, 4));
                            setName("Test Encapsulated Assertion");
                        }}
                )
        );
        // make sure admin has full access to our test entities
        for (final Entity entity : testEntities) {
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.READ, null));
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.CREATE, null));
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.UPDATE, null));
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.DELETE, null));
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.OTHER, "other"));
        }

        // mark our test entities as read-only
        protectedEntityTracker.bulkUpdateReadOnlyEntitiesList(
                Functions.map(
                        testEntities,
                        new Functions.Unary<Pair<EntityType, String>, Entity>() {
                            @Override
                            public Pair<EntityType, String> call(final Entity entity) {
                                Assert.assertNotNull(entity);
                                return Pair.pair(EntityType.findTypeByEntity(entity.getClass()), entity.getId());
                            }
                        }
                )
        );

        // now that our test entities are marked as read-only admin shouldn't have other then read permissions
        for (final Entity entity : testEntities) {
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.READ, null));
            Assert.assertFalse(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.CREATE, null));
            Assert.assertFalse(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.UPDATE, null));
            Assert.assertFalse(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.DELETE, null));
            Assert.assertFalse(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.OTHER, "other"));
        }

        // now do with entity protection disabled, admin user should have full access
        protectedEntityTracker.doWithEntityProtectionDisabled(
                new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        for (final Entity entity : testEntities) {
                            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.READ, null));
                            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.CREATE, null));
                            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.UPDATE, null));
                            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.DELETE, null));
                            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.OTHER, "other"));
                        }
                        return null;
                    }
                }
        );

        // finally try with protectedEntityTracker disabled altogether
        // disable it
        config.putProperty(ServerConfigParams.PARAM_PROTECTED_ENTITY_TRACKER_ENABLE, String.valueOf(false));
        ConfigFactory.clearCachedConfig();
        // test that protectedEntityTracker is disabled
        Assert.assertFalse(protectedEntityTracker.isEnabled());
        Assert.assertFalse(protectedEntityTracker.isEntityProtectionEnabled());
        Assert.assertThat(protectedEntityTracker.getProtectedEntities(), Matchers.anyOf(Matchers.nullValue(Map.class), Matchers.equalTo(Collections.<String, EntityProtectionInfo>emptyMap())));
        // admin user should have full access
        for (final Entity entity : testEntities) {
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.READ, null));
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.CREATE, null));
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.UPDATE, null));
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.DELETE, null));
            Assert.assertTrue(rbacServices.isPermittedForEntity(adminUser, entity, OperationType.OTHER, "other"));
        }
    }

    //- PRIVATE

    private final EntityFinder entityFinder = new EntityFinderStub();
    private final RoleManager roleManager = new MockRoleManager( entityFinder ){
            @Override
            public Collection<Role> getAssignedRoles( final User user ) throws FindException {
                if ( "some".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<>();
                    Role role = new Role();
                    role.addEntityPermission( OperationType.READ, EntityType.SERVICE, new Goid(0,123).toHexString() );
                    roles.add( role );
                    return roles;
                } else if ( "some-other".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<>();
                    Role role = new Role();
                    role.addEntityPermission( OperationType.READ, EntityType.SERVICE, new Goid(0,456).toHexString() );
                    roles.add( role );
                    return roles;
                } else if ( "any".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<>();
                    Role role = new Role();
                    role.addEntityPermission( OperationType.READ, EntityType.SERVICE, null );
                    roles.add( role );
                    return roles;
                } else if ( "other-ent".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<>();
                    Role role = new Role();
                    role.addEntityPermission( OperationType.READ, EntityType.POLICY, null );
                    roles.add( role );
                    return roles;
                } else if ( "other-perm".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<>();
                    Role role = new Role();
                    role.addEntityPermission( OperationType.UPDATE, EntityType.SERVICE, null );
                    roles.add( role );
                    return roles;
                } else if ( "admin".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<>();
                    Role role = new Role();
                    for (OperationType type : OperationType.ALL_CRUD) {
                        role.addEntityPermission(type, EntityType.ANY, null);
                    }
                    role.addEntityOtherPermission(EntityType.ANY, null, "other");
                    roles.add(role);
                    return roles;
                } else { // none
                    return Collections.emptyList();
                }
            }
        };
    
    private static UserBean newUserBean(String login) {
        UserBean ret = new UserBean(login);
        ret.setUniqueIdentifier(login);
        return ret;
    }
    
}
