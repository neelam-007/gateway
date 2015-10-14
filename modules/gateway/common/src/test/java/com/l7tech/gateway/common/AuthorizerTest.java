package com.l7tech.gateway.common;

import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.CollectionUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        user.setUniqueIdentifier(new Goid(0, -3).toString());
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

    private Authorizer buildAuthorizer(final Collection<Permission> userPermissions) {
        return buildAuthorizer(userPermissions, Collections.<String, EntityProtectionInfo>emptyMap());
    }

    private Authorizer buildAuthorizer(final Collection<Permission> userPermissions, final Map<String, EntityProtectionInfo> protectedEntities) {
        return new Authorizer() {
            @Override
            public Collection<Permission> getUserPermissions() throws RuntimeException {
                return userPermissions;
            }

            @Override
            public Map<String, EntityProtectionInfo> getProtectedEntities() throws RuntimeException {
                return protectedEntities;
            }
        };
    }

    private Authorizer buildInternalUserAuthorizer() {
        Permission upermission = new Permission(role, OperationType.UPDATE, EntityType.USER);
        upermission.setScope(Collections.<ScopePredicate>singleton(new ObjectIdentityPredicate(upermission, new Goid(0, -3).toHexString())));
        return buildAuthorizer(Arrays.asList(upermission));
    }

    private Authorizer buildInternalIdProviderAuthorizer() {
        Permission cpermission = new Permission(role, OperationType.CREATE, EntityType.USER);
        cpermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(cpermission, "providerId", "0000000000000000fffffffffffffffe")));
        Permission rpermission = new Permission(role, OperationType.READ, EntityType.USER);
        rpermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(rpermission, "providerId", "0000000000000000fffffffffffffffe")));
        Permission upermission = new Permission(role, OperationType.UPDATE, EntityType.USER);
        upermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(upermission, "providerId", "0000000000000000fffffffffffffffe")));
        Permission dpermission = new Permission(role, OperationType.DELETE, EntityType.USER);
        dpermission.setScope(Collections.<ScopePredicate>singleton(new AttributePredicate(dpermission, "providerId", "0000000000000000fffffffffffffffe")));
        return buildAuthorizer(Arrays.asList(cpermission, rpermission, upermission, dpermission));
    }


    private static final String OTHER_OPERATION = "debugger";

    @SuppressWarnings("serial")
    @Test
    public void testProtectedEntities() throws Exception {
        final Authorizer authorizer = buildAuthorizer(
                // admin user have access to everything
                CollectionUtils.list(
                        new Permission(role, OperationType.READ, EntityType.ANY),
                        new Permission(role, OperationType.CREATE, EntityType.ANY),
                        new Permission(role, OperationType.UPDATE, EntityType.ANY),
                        new Permission(role, OperationType.DELETE, EntityType.ANY),
                        new Permission(role, OperationType.OTHER, EntityType.ANY) {{ setOtherOperationName(OTHER_OPERATION); }}
                ),
                CollectionUtils.<String, EntityProtectionInfo>mapBuilder()
                        .put(new Goid(0, 1).toString(), new EntityProtectionInfo(EntityType.SERVER_MODULE_FILE, true))
                        .put(new Goid(0, 2).toString(), new EntityProtectionInfo(EntityType.POLICY, true))
                        .put(new Goid(0, 3).toString(), new EntityProtectionInfo(EntityType.SERVICE, true))
                        .put(new Goid(0, 4).toString(), new EntityProtectionInfo(EntityType.ENCAPSULATED_ASSERTION, true))
                        .map()
        );

        // admin has access to any/all variants
        Assert.assertTrue(authorizer.hasPermission(new AttemptedAnyOperation(EntityType.SERVER_MODULE_FILE)));
        Assert.assertTrue(authorizer.hasPermission(new AttemptedUpdateAll(EntityType.SERVER_MODULE_FILE)));
        Assert.assertTrue(authorizer.hasPermission(new AttemptedUpdateAny(EntityType.SERVER_MODULE_FILE)));
        Assert.assertTrue(authorizer.hasPermission(new AttemptedReadAny(EntityType.SERVER_MODULE_FILE)));
        Assert.assertTrue(authorizer.hasPermission(new AttemptedReadAll(EntityType.SERVER_MODULE_FILE)));
        Assert.assertTrue(authorizer.hasPermission(new AttemptedDeleteAll(EntityType.SERVER_MODULE_FILE)));
        Assert.assertTrue(authorizer.hasPermission(new AttemptedDeleteAll(EntityType.SERVER_MODULE_FILE)));
        Assert.assertTrue(authorizer.hasPermission(new AttemptedOther(EntityType.SERVER_MODULE_FILE, OTHER_OPERATION)));

        // test entity SERVER_MODULE_FILE with goid = new Goid(0, 1)
        doTestProtectedEntity(
                authorizer,
                new ServerModuleFile() {{
                    setGoid(new Goid(0, 1));
                }},
                EntityType.SERVER_MODULE_FILE,
                true
        );

        // test another entity SERVER_MODULE_FILE with goid = new Goid(0, 2) which is not marked as read-only
        doTestProtectedEntity(
                authorizer,
                new ServerModuleFile() {{
                    setGoid(new Goid(0, 2));
                }},
                EntityType.SERVER_MODULE_FILE,
                false
        );

        // test entity POLICY with goid = new Goid(0, 2)
        doTestProtectedEntity(
                authorizer,
                new Policy(PolicyType.INCLUDE_FRAGMENT, "policy name", null, false) {{
                    setGoid(new Goid(0, 2));
                }},
                EntityType.POLICY,
                true
        );

        // test another entity POLICY with goid = new Goid(0, 1) which is not marked as read-only
        doTestProtectedEntity(
                authorizer,
                new Policy(PolicyType.INCLUDE_FRAGMENT, "policy name", null, false) {{
                    setGoid(new Goid(0, 1));
                }},
                EntityType.POLICY,
                false
        );

        // test entity SERVICE with goid = new Goid(0, 3)
        doTestProtectedEntity(
                authorizer,
                new PublishedService() {{
                    setGoid(new Goid(0, 3));
                }},
                EntityType.SERVICE,
                true
        );

        // test another entity SERVICE with goid = new Goid(0, 1) which is not marked as read-only
        doTestProtectedEntity(
                authorizer,
                new PublishedService() {{
                    setGoid(new Goid(0, 1));
                }},
                EntityType.SERVICE,
                false
        );

        // test read-only service with existing read-only policy
        doTestProtectedEntity(
                authorizer,
                new PublishedService() {{
                    setGoid(new Goid(0, 3));
                    setPolicy(
                            new Policy(PolicyType.INCLUDE_FRAGMENT, "policy name", null, false) {{
                                setGoid(new Goid(0, 2));
                            }}
                    );
                }},
                EntityType.SERVICE,
                true
        );

        // test non read-only service with existing read-only policy
        doTestProtectedEntity(
                authorizer,
                new PublishedService() {{
                    setGoid(new Goid(0, 1));
                    setPolicy(
                            new Policy(PolicyType.INCLUDE_FRAGMENT, "policy name", null, false) {{
                                setGoid(new Goid(0, 2));
                            }}
                    );
                }},
                EntityType.SERVICE,
                false
        );

        // test non read-only service with existing non read-only policy
        doTestProtectedEntity(
                authorizer,
                new PublishedService() {{
                    setGoid(new Goid(0, 1));
                    setPolicy(
                            new Policy(PolicyType.INCLUDE_FRAGMENT, "policy name", null, false) {{
                                setGoid(new Goid(0, 3));
                            }}
                    );
                }},
                EntityType.SERVICE,
                false
        );

        // test entity ENCAPSULATED_ASSERTION with goid = new Goid(0, 4)
        doTestProtectedEntity(
                authorizer,
                new EncapsulatedAssertionConfig() {{
                    setGoid(new Goid(0, 4));
                }},
                EntityType.ENCAPSULATED_ASSERTION,
                true
        );

        // test another entity ENCAPSULATED_ASSERTION with goid = new Goid(0, 3) which is not marked as read-only
        doTestProtectedEntity(
                authorizer,
                new EncapsulatedAssertionConfig() {{
                    setGoid(new Goid(0, 3));
                }},
                EntityType.ENCAPSULATED_ASSERTION,
                false
        );

        // test read-only encaps with existing read-only policy
        doTestProtectedEntity(
                authorizer,
                new EncapsulatedAssertionConfig() {{
                    setGoid(new Goid(0, 4));
                    setPolicy(
                            new Policy(PolicyType.INCLUDE_FRAGMENT, "policy name", null, false) {{
                                setGoid(new Goid(0, 2));
                            }}
                    );
                }},
                EntityType.ENCAPSULATED_ASSERTION,
                true
        );

        // test non read-only encaps with existing read-only policy
        doTestProtectedEntity(
                authorizer,
                new EncapsulatedAssertionConfig() {{
                    setGoid(new Goid(0, 3));
                    setPolicy(
                            new Policy(PolicyType.INCLUDE_FRAGMENT, "policy name", null, false) {{
                                setGoid(new Goid(0, 2));
                            }}
                    );
                }},
                EntityType.ENCAPSULATED_ASSERTION,
                false
        );

        // test non read-only encaps with existing non read-only policy
        doTestProtectedEntity(
                authorizer,
                new EncapsulatedAssertionConfig() {{
                    setGoid(new Goid(0, 3));
                    setPolicy(
                            new Policy(PolicyType.INCLUDE_FRAGMENT, "policy name", null, false) {{
                                setGoid(new Goid(0, 1));
                            }}
                    );
                }},
                EntityType.ENCAPSULATED_ASSERTION,
                false
        );
    }

    private static void doTestProtectedEntity(final Authorizer authorizer, final Entity entity, final EntityType entityType, final boolean readOnly) {
        Assert.assertNotNull(authorizer);
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entityType);

        // read is always OK
        Assert.assertTrue(authorizer.hasPermission(new AttemptedReadSpecific(entityType, entity)));

        // create, update, delete and other should fail if readOnly
        Assert.assertThat(
                authorizer.hasPermission(new AttemptedCreateSpecific(entityType, entity)),
                Matchers.is(!readOnly)
        );
        Assert.assertThat(
                authorizer.hasPermission(new AttemptedUpdate(entityType, entity)),
                Matchers.is(!readOnly)
        );
        Assert.assertThat(
                authorizer.hasPermission(new AttemptedDeleteSpecific(entityType, entity)),
                Matchers.is(!readOnly)
        );
        Assert.assertThat(
                authorizer.hasPermission(new AttemptedOtherSpecific(entityType, entity, OTHER_OPERATION)),
                Matchers.is(!readOnly)
        );
    }
}

