package com.l7tech.gateway.common;

import org.junit.Test;
import org.junit.Assert;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.AttributePredicate;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.ObjectIdentityPredicate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserBean;

import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;

/**
 * Authorizer tests
 */
public class AuthorizerTest {

    @Test
    public void testAttributePredicateCreate() {
        Authorizer authorizer = buildInternalIdProviderAuthorizer();
        Assert.assertTrue( "Should be permitted", authorizer.hasPermission(new AttemptedCreateSpecific(EntityType.USER, new UserBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, "<new user>")) ));
        Assert.assertFalse( "Should not be permitted", authorizer.hasPermission(new AttemptedCreateSpecific(EntityType.USER, new UserBean(1, "<new user>") )));
    }

    @Test
    public void testAttributePredicateUpdate() {
        Authorizer authorizer = buildInternalIdProviderAuthorizer();
        Assert.assertTrue( "Should be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, new UserBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, "<new user>")) ));
        Assert.assertFalse( "Should not be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, new UserBean(1, "<new user>") )));
    }

    @Test
    public void testAttributePredicateDelete() {
        Authorizer authorizer = buildInternalIdProviderAuthorizer();
        Assert.assertTrue( "Should be permitted", authorizer.hasPermission(new AttemptedDeleteSpecific(EntityType.USER, new UserBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, "<new user>")) ));
        Assert.assertFalse( "Should not be permitted", authorizer.hasPermission(new AttemptedDeleteSpecific(EntityType.USER, new UserBean(1, "<new user>") )));
    }

    @Test
    public void testIdentityPredicateNoId() {
        Authorizer authorizer = buildInternalUserAuthorizer();
        UserBean user = new UserBean(1, "<admin user>");
        user.setUniqueIdentifier( "-3" );
        Assert.assertTrue( "Should be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, user )) );
        Assert.assertFalse( "Should not be permitted", authorizer.hasPermission(new AttemptedUpdate(EntityType.USER, new UserBean(1, "<other user>") )) );
    }

    private Authorizer buildInternalUserAuthorizer() {
        return new Authorizer() {
            public Collection<Permission> getUserPermissions() throws RuntimeException {
                Role role = new Role();
                Permission upermission = new Permission( role, OperationType.UPDATE, EntityType.USER );
                upermission.setScope( Collections.<ScopePredicate>singleton( new ObjectIdentityPredicate( upermission, -3 ) ) );
                return Arrays.asList( upermission );
            }
        };
    }

    private Authorizer buildInternalIdProviderAuthorizer() {
        return new Authorizer() {
            public Collection<Permission> getUserPermissions() throws RuntimeException {
                Role role = new Role();
                Permission cpermission = new Permission( role, OperationType.CREATE, EntityType.USER );
                cpermission.setScope( Collections.<ScopePredicate>singleton( new AttributePredicate( cpermission, "providerId", "-2") ) );
                Permission rpermission = new Permission( role, OperationType.READ, EntityType.USER );
                rpermission.setScope( Collections.<ScopePredicate>singleton( new AttributePredicate( rpermission, "providerId", "-2") ) );
                Permission upermission = new Permission( role, OperationType.UPDATE, EntityType.USER );
                upermission.setScope( Collections.<ScopePredicate>singleton( new AttributePredicate( upermission, "providerId", "-2") ) );
                Permission dpermission = new Permission( role, OperationType.DELETE, EntityType.USER );
                dpermission.setScope( Collections.<ScopePredicate>singleton( new AttributePredicate( dpermission, "providerId", "-2") ) );
                return Arrays.asList( cpermission, rpermission, upermission, dpermission );
            }
        };
    }
}

