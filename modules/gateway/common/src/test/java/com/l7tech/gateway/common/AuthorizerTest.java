package com.l7tech.gateway.common;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Authorizer tests
 */
public class AuthorizerTest {
    private Authorizer authorizer;
    private Collection<Permission> userPermissions;

    @Before
    public void setup() {
        userPermissions = new HashSet<>();
        authorizer = new AuthorizerStub(userPermissions);
    }

    @Test
    public void testAttributePredicateCreate() {
        Authorizer authorizer = buildInternalIdProviderAuthorizer();
        assertTrue("Should be permitted", authorizer.hasPermission(new AttemptedCreateSpecific(EntityType.USER, new UserBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, "<new user>"))));
        assertFalse("Should not be permitted", authorizer.hasPermission(new AttemptedCreateSpecific(EntityType.USER, new UserBean(1, "<new user>"))));
    }

    @Test
    public void testAttributePredicateUpdate() {
        Authorizer authorizer = buildInternalIdProviderAuthorizer();
        assertTrue("Should be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, new UserBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, "<new user>"))));
        assertFalse("Should not be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, new UserBean(1, "<new user>"))));
    }

    @Test
    public void testAttributePredicateDelete() {
        Authorizer authorizer = buildInternalIdProviderAuthorizer();
        assertTrue("Should be permitted", authorizer.hasPermission(new AttemptedDeleteSpecific(EntityType.USER, new UserBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, "<new user>"))));
        assertFalse("Should not be permitted", authorizer.hasPermission(new AttemptedDeleteSpecific(EntityType.USER, new UserBean(1, "<new user>"))));
    }

    @Test
    public void testIdentityPredicateNoId() {
        Authorizer authorizer = buildInternalUserAuthorizer();
        UserBean user = new UserBean(1, "<admin user>");
        user.setUniqueIdentifier("-3");
        assertTrue("Should be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, user)));
        assertFalse("Should not be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, new UserBean(1, "<other user>"))));
    }

    @BugId("SSM-3871")
    @Test
    public void attemptedCreateWithSecurityZonePredicate() {
        final Permission createPermission = new Permission(new Role(), OperationType.CREATE, EntityType.ANY);
        final Set<ScopePredicate> scope = new HashSet<>();
        final SecurityZone zone = new SecurityZone();
        // entity type matches
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.ID_PROVIDER_CONFIG));
        scope.add(new SecurityZonePredicate(createPermission, zone));
        createPermission.setScope(scope);
        userPermissions.add(createPermission);

        assertTrue(authorizer.hasPermission(new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG)));
    }

    @BugId("SSM-3871")
    @Test
    public void attemptedCreateWithSecurityZonePredicateThatAllowsAnyEntityType() {
        final Permission createPermission = new Permission(new Role(), OperationType.CREATE, EntityType.ANY);
        final Set<ScopePredicate> scope = new HashSet<>();
        final SecurityZone zone = new SecurityZone();
        // any type is allowed
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.ANY));
        scope.add(new SecurityZonePredicate(createPermission, zone));
        createPermission.setScope(scope);
        userPermissions.add(createPermission);

        assertTrue(authorizer.hasPermission(new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG)));
    }

    @BugId("SSM-3871")
    @Test
    public void doesNotHavePermissionAttemptedCreateWithSecurityZonePredicate() {
        final Permission createPermission = new Permission(new Role(), OperationType.CREATE, EntityType.ANY);
        final Set<ScopePredicate> scope = new HashSet<>();
        final SecurityZone zone = new SecurityZone();
        // entity type mismatch
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.POLICY));
        scope.add(new SecurityZonePredicate(createPermission, zone));
        createPermission.setScope(scope);
        userPermissions.add(createPermission);

        assertFalse(authorizer.hasPermission(new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG)));
    }

    @BugId("SSM-3871")
    @Test
    public void attemptedCreateWithMixedPredicates() {
        final Permission createPermission = new Permission(new Role(), OperationType.CREATE, EntityType.ANY);
        final Set<ScopePredicate> scope = new HashSet<>();
        final SecurityZone zone = new SecurityZone();
        // entity type mismatch
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.POLICY));
        scope.add(new SecurityZonePredicate(createPermission, zone));
        // non-zone predicate
        scope.add(new AttributePredicate(createPermission, "name", "test"));
        createPermission.setScope(scope);
        userPermissions.add(createPermission);

        assertTrue(authorizer.hasPermission(new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG)));
    }

    @BugId("SSM-3871")
    @Test
    public void attemptedCreateWithNoSecurityZonePredicates() {
        final Permission createPermission = new Permission(new Role(), OperationType.CREATE, EntityType.ANY);
        final Set<ScopePredicate> scope = new HashSet<>();
        scope.add(new AttributePredicate(createPermission, "name", "test"));
        createPermission.setScope(scope);
        userPermissions.add(createPermission);

        assertTrue(authorizer.hasPermission(new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG)));
    }

    @BugId("SSM-3871")
    @Test
    public void attemptedCreateSpecificEntityTypePermissionNoScope() {
        userPermissions.add(new Permission(new Role(), OperationType.CREATE, EntityType.ID_PROVIDER_CONFIG));
        assertTrue(authorizer.hasPermission(new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG)));
    }

    @BugId("SSM-3871")
    @Test
    public void attemptedCreateSpecificEntityTypePermissionWithSecurityZonePredicate() {
        final Permission createPermission = new Permission(new Role(), OperationType.CREATE, EntityType.ID_PROVIDER_CONFIG);
        final Set<ScopePredicate> scope = new HashSet<>();
        final SecurityZone zone = new SecurityZone();
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.ID_PROVIDER_CONFIG));
        scope.add(new SecurityZonePredicate(createPermission, zone));
        createPermission.setScope(scope);
        userPermissions.add(createPermission);

        assertTrue(authorizer.hasPermission(new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG)));
    }

    @BugId("SSM-3871")
    @Test
    public void attemptedCreateSpecificEntityTypePermissionWithNoSecurityZonePredicates() {
        final Permission createPermission = new Permission(new Role(), OperationType.CREATE, EntityType.ID_PROVIDER_CONFIG);
        final Set<ScopePredicate> scope = new HashSet<>();
        scope.add(new AttributePredicate(createPermission, "name", "test"));
        createPermission.setScope(scope);
        userPermissions.add(createPermission);

        assertTrue(authorizer.hasPermission(new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG)));
    }

    @BugId("SSM-3871")
    @Test
    public void attemptedCreateSpecificEntityTypePermissionWithMixedPredicates() {
        final Permission createPermission = new Permission(new Role(), OperationType.CREATE, EntityType.ID_PROVIDER_CONFIG);
        final Set<ScopePredicate> scope = new HashSet<>();
        final SecurityZone zone = new SecurityZone();
        // entity type mismatch
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.POLICY));
        scope.add(new SecurityZonePredicate(createPermission, zone));
        // non-zone predicate
        scope.add(new AttributePredicate(createPermission, "name", "test"));
        createPermission.setScope(scope);
        userPermissions.add(createPermission);

        assertTrue(authorizer.hasPermission(new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG)));
    }

    @BugId("SSM-3871")
    @Test
    public void doesNotHavePermissionAttemptedCreateSpecificEntityTypePermissionWithSecurityZonePredicate() {
        final Permission createPermission = new Permission(new Role(), OperationType.CREATE, EntityType.ID_PROVIDER_CONFIG);
        final Set<ScopePredicate> scope = new HashSet<>();
        final SecurityZone zone = new SecurityZone();
        // entity type mismatch
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.POLICY));
        scope.add(new SecurityZonePredicate(createPermission, zone));
        createPermission.setScope(scope);
        userPermissions.add(createPermission);

        assertFalse(authorizer.hasPermission(new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG)));
    }

    private Authorizer buildInternalUserAuthorizer() {
        return new Authorizer() {
            public Collection<Permission> getUserPermissions() throws RuntimeException {
                Role role = new Role();
                Permission upermission = new Permission(role, OperationType.UPDATE, EntityType.USER);
                upermission.setScope(Collections.<ScopePredicate>singleton(new ObjectIdentityPredicate(upermission, -3)));
                return Arrays.asList(upermission);
            }
        };
    }

    private Authorizer buildInternalIdProviderAuthorizer() {
        return new Authorizer() {
            public Collection<Permission> getUserPermissions() throws RuntimeException {
                Role role = new Role();
                Permission cpermission = new Permission(role, OperationType.CREATE, EntityType.USER);
                cpermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(cpermission, "providerId", "-2")));
                Permission rpermission = new Permission(role, OperationType.READ, EntityType.USER);
                rpermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(rpermission, "providerId", "-2")));
                Permission upermission = new Permission(role, OperationType.UPDATE, EntityType.USER);
                upermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(upermission, "providerId", "-2")));
                Permission dpermission = new Permission(role, OperationType.DELETE, EntityType.USER);
                dpermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(dpermission, "providerId", "-2")));
                return Arrays.asList(cpermission, rpermission, upermission, dpermission);
            }
        };
    }

    private class AuthorizerStub extends Authorizer {
        private Collection<Permission> userPermissions;

        private AuthorizerStub(final Collection<Permission> userPermissions) {
            this.userPermissions = userPermissions;
        }

        @Override
        public Collection<Permission> getUserPermissions() throws RuntimeException {
            return userPermissions;
        }
    }
}

