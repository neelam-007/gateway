package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.EntityFinder;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZoneUpdateSecurityCheckerTest {
    private static final Goid ZONE_GOID = new Goid(0, 1234L);
    private ZoneUpdateSecurityChecker checker;
    @Mock
    private EntityFinder entityFinder;
    @Mock
    private RbacServices rbacServices;
    private List<Serializable> ids;
    private Map<EntityType, Collection<Serializable>> idMap;
    private InternalUser user;
    private SecurityZone zone;
    private Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
    private JmsConnection connection;
    private JmsEndpoint endpoint;
    private Method bulkUpdate;

    @Before
    public void setup() throws Exception {
        checker = new ZoneUpdateSecurityCheckerImpl();
        ApplicationContexts.inject(checker, CollectionUtils.<String, Object>mapBuilder()
                .put("entityFinder", entityFinder)
                .put("rbacServices", rbacServices)
                .unmodifiableMap(), false);
        ids = new ArrayList<>();
        idMap = new HashMap<>();
        user = new InternalUser("test");
        zone = new SecurityZone();
        connection = new JmsConnection();
        endpoint = new JmsEndpoint();
    }

    @Test
    public void checkUpdatePermitted() throws Throwable {
        ids.add(1L);
        ids.add(2L);
        when(entityFinder.find(Policy.class, 1L)).thenReturn(policy);
        final Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "test2", "test2", false);
        when(entityFinder.find(Policy.class, 2L)).thenReturn(policy2);
        when(rbacServices.isPermittedForEntity(eq(user), any(Policy.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        checker.checkBulkUpdatePermitted(user, null, EntityType.POLICY, ids);

        verify(entityFinder, never()).find(eq(SecurityZone.class), anyLong());
        // once for pre-edit check, once for after edit check
        verify(rbacServices, times(2)).isPermittedForEntity(user, policy, OperationType.UPDATE, null);
        verify(rbacServices, times(2)).isPermittedForEntity(user, policy2, OperationType.UPDATE, null);
    }

    @Test
    public void checkUpdatePermittedGoid() throws Throwable {
        Goid goid1 = new Goid(0, 1);
        Goid goid2 = new Goid(0, 2);
        ids.add(goid1);
        ids.add(goid2);
        when(entityFinder.find(Policy.class, goid1)).thenReturn(policy);
        final Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "test2", "test2", false);
        when(entityFinder.find(Policy.class, goid2)).thenReturn(policy2);
        when(rbacServices.isPermittedForEntity(eq(user), any(Policy.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        checker.checkBulkUpdatePermitted(user, null, EntityType.POLICY, ids);

        verify(entityFinder, never()).find(eq(SecurityZone.class), any(Goid.class));
        // once for pre-edit check, once for after edit check
        verify(rbacServices, times(2)).isPermittedForEntity(user, policy, OperationType.UPDATE, null);
        verify(rbacServices, times(2)).isPermittedForEntity(user, policy2, OperationType.UPDATE, null);
    }

    @Test
    public void nonNullSecurityZoneOidLooksUpZone() throws Throwable {
        ids.add(1L);
        when(entityFinder.find(SecurityZone.class, ZONE_GOID)).thenReturn(zone);
        when(entityFinder.find(Policy.class, 1L)).thenReturn(policy);
        when(rbacServices.isPermittedForEntity(eq(user), any(Policy.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        checker.checkBulkUpdatePermitted(user, ZONE_GOID, EntityType.POLICY, ids);

        verify(entityFinder).find(SecurityZone.class, ZONE_GOID);
        // once for pre-edit check, once for after edit check
        verify(rbacServices, times(2)).isPermittedForEntity(user, policy, OperationType.UPDATE, null);
    }

    @Test
    public void nonNullSecurityZoneOidLooksUpZoneGoid() throws Throwable {
        Goid goid1 = new Goid(0, 1);
        ids.add(goid1);
        when(entityFinder.find(SecurityZone.class, ZONE_GOID)).thenReturn(zone);
        when(entityFinder.find(Policy.class, goid1)).thenReturn(policy);
        when(rbacServices.isPermittedForEntity(eq(user), any(Policy.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        checker.checkBulkUpdatePermitted(user, ZONE_GOID, EntityType.POLICY, ids);

        verify(entityFinder).find(SecurityZone.class, ZONE_GOID);
        // once for pre-edit check, once for after edit check
        verify(rbacServices, times(2)).isPermittedForEntity(user, policy, OperationType.UPDATE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonZoneableEntityType() throws Throwable {
        checker.checkBulkUpdatePermitted(user, null, EntityType.RBAC_ROLE, ids);
    }

    @Test(expected = IllegalArgumentException.class)
    public void securityZoneDoesNotExist() throws Throwable {
        when(entityFinder.find(SecurityZone.class, ZONE_GOID)).thenReturn(null);
        checker.checkBulkUpdatePermitted(user, ZONE_GOID, EntityType.POLICY, ids);
    }

    @Test(expected = IllegalArgumentException.class)
    public void atLeastOneEntityDoesNotExist() throws Throwable {
        ids.add(1L);
        ids.add(2L);
        when(entityFinder.find(Policy.class, 1L)).thenReturn(policy);
        when(entityFinder.find(Policy.class, 2L)).thenReturn(null);
        when(rbacServices.isPermittedForEntity(eq(user), any(Policy.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        try {
            checker.checkBulkUpdatePermitted(user, null, EntityType.POLICY, ids);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Entity with id 2 does not exist or is not Security Zoneable", e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void atLeastOneGoidEntityDoesNotExist() throws Throwable {
        Goid goid1 = new Goid(0, 1);
        Goid goid2 = new Goid(0, 2);
        ids.add(goid1);
        ids.add(goid2);
        when(entityFinder.find(Policy.class, goid1)).thenReturn(policy);
        when(entityFinder.find(Policy.class, goid2)).thenReturn(null);
        when(rbacServices.isPermittedForEntity(eq(user), any(Policy.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        try {
            checker.checkBulkUpdatePermitted(user, null, EntityType.POLICY, ids);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Entity with id " + goid2.toString() + " does not exist or is not Security Zoneable", e.getMessage());
            throw e;
        }
    }

    @Test(expected = PermissionDeniedException.class)
    public void atLeastOnePermissionDeniedForUpdate() throws Throwable {
        ids.add(1L);
        ids.add(2L);
        when(entityFinder.find(Policy.class, 1L)).thenReturn(policy);
        final Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "test2", "test2", false);
        when(entityFinder.find(Policy.class, 2L)).thenReturn(policy2);
        when(rbacServices.isPermittedForEntity(user, policy, OperationType.UPDATE, null)).thenReturn(true);
        when(rbacServices.isPermittedForEntity(user, policy2, OperationType.UPDATE, null)).thenReturn(false);

        try {
            checker.checkBulkUpdatePermitted(user, null, EntityType.POLICY, ids);
            fail("Expected PermissionDeniedException");
        } catch (final PermissionDeniedException e) {
            assertEquals(OperationType.UPDATE, e.getOperation());
            assertEquals(policy2, e.getEntity());
            throw e;
        }
    }

    @Test
    public void checkUpdatePermittedForMultipleEntityTypes() throws Throwable {
        idMap.put(EntityType.JMS_CONNECTION, Collections.<Serializable>singleton(1L));
        idMap.put(EntityType.JMS_ENDPOINT, Collections.<Serializable>singleton(2L));
        when(entityFinder.find(JmsConnection.class, 1L)).thenReturn(connection);
        when(entityFinder.find(JmsEndpoint.class, 2L)).thenReturn(endpoint);
        when(rbacServices.isPermittedForEntity(eq(user), any(JmsConnection.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);
        when(rbacServices.isPermittedForEntity(eq(user), any(JmsEndpoint.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        checker.checkBulkUpdatePermitted(user, null, idMap);

        // once for pre-edit check, once for after edit check
        verify(rbacServices, times(2)).isPermittedForEntity(user, connection, OperationType.UPDATE, null);
        verify(rbacServices, times(2)).isPermittedForEntity(user, endpoint, OperationType.UPDATE, null);
    }

    @Test
    public void checkUpdatePermittedForMultipleGoidEntityTypes() throws Throwable {
        Goid goid1 = new Goid(0, 1);
        Goid goid2 = new Goid(0, 2);
        idMap.put(EntityType.JMS_CONNECTION, Collections.<Serializable>singleton(goid1));
        idMap.put(EntityType.JMS_ENDPOINT, Collections.<Serializable>singleton(goid2));
        when(entityFinder.find(JmsConnection.class, goid1)).thenReturn(connection);
        when(entityFinder.find(JmsEndpoint.class, goid2)).thenReturn(endpoint);
        when(rbacServices.isPermittedForEntity(eq(user), any(JmsConnection.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);
        when(rbacServices.isPermittedForEntity(eq(user), any(JmsEndpoint.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        checker.checkBulkUpdatePermitted(user, null, idMap);

        // once for pre-edit check, once for after edit check
        verify(rbacServices, times(2)).isPermittedForEntity(user, connection, OperationType.UPDATE, null);
        verify(rbacServices, times(2)).isPermittedForEntity(user, endpoint, OperationType.UPDATE, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void atLeastOnePermissionDeniedForUpdateMultipleEntityTypes() throws Throwable {
        idMap.put(EntityType.JMS_CONNECTION, Collections.<Serializable>singleton(1L));
        idMap.put(EntityType.JMS_ENDPOINT, Collections.<Serializable>singleton(2L));
        when(entityFinder.find(JmsConnection.class, 1L)).thenReturn(connection);
        when(entityFinder.find(JmsEndpoint.class, 2L)).thenReturn(endpoint);
        when(rbacServices.isPermittedForEntity(eq(user), any(Entity.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(false);

        try {
            checker.checkBulkUpdatePermitted(user, null, idMap);
            fail("Expected PermissionDeniedException");
        } catch (final PermissionDeniedException e) {
            assertEquals(OperationType.UPDATE, e.getOperation());
            assertTrue(e.getEntity().equals(connection) || e.getEntity().equals(endpoint));
            throw e;
        }
    }

    /**
     * If user can modify any entity of that type, don't need to cycle through each entity to determine if permission is allowed.
     */
    @Test
    public void checkUpdatePermittedForAnyEntityOfType() throws Throwable {
        ids.add(1L);
        when(rbacServices.isPermittedForAnyEntityOfType(user, OperationType.UPDATE, EntityType.POLICY)).thenReturn(true);

        checker.checkBulkUpdatePermitted(user, null, EntityType.POLICY, ids);

        verify(entityFinder, never()).find(any(Class.class), anyLong());
        verify(rbacServices, never()).isPermittedForEntity(any(User.class), any(Entity.class), any(OperationType.class), anyString());
    }
}
