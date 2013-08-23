package com.l7tech.gateway.common;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Authorizer tests
 */
public class AuthorizerTest {
    private Set<Permission> permissions;
    private UserBean user;
    private Role role;

    @Before
    public void setup() {
        user = new UserBean(new Goid(0, 1), "<admin user>");
        user.setUniqueIdentifier("-3");
        role = new Role();
        permissions = new HashSet<>();
    }

    @Test
    public void testAttributePredicateCreate() {
        Authorizer authorizer = buildInternalIdProviderAuthorizer();
        Assert.assertTrue("Should be permitted", authorizer.hasPermission(new AttemptedCreateSpecific(EntityType.USER, new UserBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, "<new user>"))));
        Assert.assertFalse("Should not be permitted", authorizer.hasPermission(new AttemptedCreateSpecific(EntityType.USER, new UserBean(new Goid(0, 1), "<new user>"))));
    }

    @Test
    public void testAttributePredicateUpdate() {
        Authorizer authorizer = buildInternalIdProviderAuthorizer();
        Assert.assertTrue("Should be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, new UserBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, "<new user>"))));
        Assert.assertFalse("Should not be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, new UserBean(new Goid(0, 1), "<new user>"))));
    }

    @Test
    public void testAttributePredicateDelete() {
        Authorizer authorizer = buildInternalIdProviderAuthorizer();
        Assert.assertTrue("Should be permitted", authorizer.hasPermission(new AttemptedDeleteSpecific(EntityType.USER, new UserBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, "<new user>"))));
        Assert.assertFalse("Should not be permitted", authorizer.hasPermission(new AttemptedDeleteSpecific(EntityType.USER, new UserBean(new Goid(0, 1), "<new user>"))));
    }

    @Test
    public void testIdentityPredicateNoId() {
        Authorizer authorizer = buildInternalUserAuthorizer();
        Assert.assertTrue("Should be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, user)));
        Assert.assertFalse("Should not be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, new UserBean(new Goid(0, 1), "<other user>"))));
    }

    @Test
    public void hasPermissionAttemptedEntityOperationMoreThanOnePredicate() {
        final Folder parentFolder = new Folder("parent", null);
        final Folder childFolder = new Folder("child", parentFolder);
        final Permission updatePermission = new Permission(role, OperationType.UPDATE, EntityType.ANY);
        final FolderPredicate folderPredicate = new FolderPredicate(updatePermission, parentFolder, true);
        final SecurityZonePredicate zonePredicate = new SecurityZonePredicate(updatePermission, null);
        updatePermission.getScope().add(folderPredicate);
        updatePermission.getScope().add(zonePredicate);
        permissions.add(updatePermission);
        final Authorizer authorizer = buildAuthorizer(permissions);
        assertTrue(authorizer.hasPermission(new AttemptedUpdate(EntityType.FOLDER, childFolder)));
    }

    @Test
    public void doesNotHavePermissionAttemptedEntityOperationMoreThanOnePredicate() {
        final Folder parentFolder = new Folder("parent", null);
        final Folder childFolder = new Folder("child", parentFolder);
        // fails zone predicate
        childFolder.setSecurityZone(new SecurityZone());
        final Permission updatePermission = new Permission(role, OperationType.UPDATE, EntityType.ANY);
        final FolderPredicate folderPredicate = new FolderPredicate(updatePermission, parentFolder, true);
        final SecurityZonePredicate zonePredicate = new SecurityZonePredicate(updatePermission, null);
        updatePermission.getScope().add(folderPredicate);
        updatePermission.getScope().add(zonePredicate);
        permissions.add(updatePermission);
        final Authorizer authorizer = buildAuthorizer(permissions);
        assertFalse(authorizer.hasPermission(new AttemptedUpdate(EntityType.FOLDER, childFolder)));
    }

    @Test
    public void hasPermissionAttemptedEntityOperationNoScope() {
        final Folder parentFolder = new Folder("parent", null);
        final Folder childFolder = new Folder("child", parentFolder);
        final Permission updatePermission = new Permission(role, OperationType.UPDATE, EntityType.ANY);
        updatePermission.getScope().clear();
        permissions.add(updatePermission);
        final Authorizer authorizer = buildAuthorizer(permissions);
        assertTrue(authorizer.hasPermission(new AttemptedUpdate(EntityType.FOLDER, childFolder)));
    }

    @Test
    public void hasPermissionAttemptedEntityOperationNullScope() {
        final Folder parentFolder = new Folder("parent", null);
        final Folder childFolder = new Folder("child", parentFolder);
        final Permission updatePermission = new Permission(role, OperationType.UPDATE, EntityType.ANY);
        updatePermission.setScope(null);
        permissions.add(updatePermission);
        final Authorizer authorizer = buildAuthorizer(permissions);
        assertTrue(authorizer.hasPermission(new AttemptedUpdate(EntityType.FOLDER, childFolder)));
    }

    private Authorizer buildAuthorizer(final Set<Permission> userPermissions) {
        return new Authorizer() {
            public Collection<Permission> getUserPermissions() throws RuntimeException {
                return userPermissions;
            }
        };
    }

    private Authorizer buildInternalUserAuthorizer() {
        return new Authorizer() {
            public Collection<Permission> getUserPermissions() throws RuntimeException {
                Permission upermission = new Permission(role, OperationType.UPDATE, EntityType.USER);
                upermission.setScope(Collections.<ScopePredicate>singleton(new ObjectIdentityPredicate(upermission, -3)));
                return Arrays.asList(upermission);
            }
        };
    }

    private Authorizer buildInternalIdProviderAuthorizer() {
        return new Authorizer() {
            public Collection<Permission> getUserPermissions() throws RuntimeException {
                Permission cpermission = new Permission(role, OperationType.CREATE, EntityType.USER);
                cpermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(cpermission, "providerId", "0000000000000000fffffffffffffffe")));
                Permission rpermission = new Permission(role, OperationType.READ, EntityType.USER);
                rpermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(rpermission, "providerId", "0000000000000000fffffffffffffffe")));
                Permission upermission = new Permission(role, OperationType.UPDATE, EntityType.USER);
                upermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(upermission, "providerId", "0000000000000000fffffffffffffffe")));
                Permission dpermission = new Permission(role, OperationType.DELETE, EntityType.USER);
                dpermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(dpermission, "providerId", "0000000000000000fffffffffffffffe")));
                return Arrays.asList(cpermission, rpermission, upermission, dpermission);
            }
        };
    }
}

