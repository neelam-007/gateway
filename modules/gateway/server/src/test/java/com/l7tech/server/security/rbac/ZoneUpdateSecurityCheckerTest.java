package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.EntityType;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZoneUpdateSecurityCheckerTest {
    private static final long ZONE_OID = 1234L;
    private ZoneUpdateSecurityChecker checker;
    @Mock
    private EntityFinder entityFinder;
    @Mock
    private RbacServices rbacServices;
    private List<Long> oids;
    private InternalUser user;
    private SecurityZone zone;
    private Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
    private Method bulkUpdate;

    @Before
    public void setup() throws Exception {
        checker = new ZoneUpdateSecurityCheckerImpl();
        ApplicationContexts.inject(checker, CollectionUtils.<String, Object>mapBuilder()
                .put("entityFinder", entityFinder)
                .put("rbacServices", rbacServices)
                .unmodifiableMap(), false);
        oids = new ArrayList<>();
        user = new InternalUser("test");
        zone = new SecurityZone();
    }

    @Test
    public void checkUpdatePermitted() throws Throwable {
        oids.add(1L);
        oids.add(2L);
        when(entityFinder.find(Policy.class, 1L)).thenReturn(policy);
        final Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "test2", "test2", false);
        when(entityFinder.find(Policy.class, 2L)).thenReturn(policy2);
        when(rbacServices.isPermittedForEntity(eq(user), any(Policy.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        checker.checkBulkUpdatePermitted(user, null, EntityType.POLICY, oids);

        verify(entityFinder, never()).find(eq(SecurityZone.class), anyLong());
        // once for pre-edit check, once for after edit check
        verify(rbacServices, times(2)).isPermittedForEntity(user, policy, OperationType.UPDATE, null);
        verify(rbacServices, times(2)).isPermittedForEntity(user, policy2, OperationType.UPDATE, null);
    }

    @Test
    public void nonNullSecurityZoneOidLooksUpZone() throws Throwable {
        oids.add(1L);
        when(entityFinder.find(SecurityZone.class, ZONE_OID)).thenReturn(zone);
        when(entityFinder.find(Policy.class, 1L)).thenReturn(policy);
        when(rbacServices.isPermittedForEntity(eq(user), any(Policy.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        checker.checkBulkUpdatePermitted(user, ZONE_OID, EntityType.POLICY, oids);

        verify(entityFinder).find(SecurityZone.class, ZONE_OID);
        // once for pre-edit check, once for after edit check
        verify(rbacServices, times(2)).isPermittedForEntity(user, policy, OperationType.UPDATE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonZoneableEntityType() throws Throwable {
        checker.checkBulkUpdatePermitted(user, null, EntityType.RBAC_ROLE, oids);
    }

    @Test(expected = IllegalArgumentException.class)
    public void securityZoneDoesNotExist() throws Throwable {
        when(entityFinder.find(SecurityZone.class, ZONE_OID)).thenReturn(null);
        checker.checkBulkUpdatePermitted(user, ZONE_OID, EntityType.POLICY, oids);
    }

    @Test(expected = IllegalArgumentException.class)
    public void atLeastOneEntityDoesNotExist() throws Throwable {
        oids.add(1L);
        oids.add(2L);
        when(entityFinder.find(Policy.class, 1L)).thenReturn(policy);
        when(entityFinder.find(Policy.class, 2L)).thenReturn(null);
        when(rbacServices.isPermittedForEntity(eq(user), any(Policy.class), eq(OperationType.UPDATE), eq((String) null))).thenReturn(true);

        try {
            checker.checkBulkUpdatePermitted(user, null, EntityType.POLICY, oids);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Entity with oid 2 does not exist or is not Security Zoneable", e.getMessage());
            throw e;
        }
    }

    @Test(expected = PermissionDeniedException.class)
    public void atLeastOnePermissionDeniedForUpdate() throws Throwable {
        oids.add(1L);
        oids.add(2L);
        when(entityFinder.find(Policy.class, 1L)).thenReturn(policy);
        final Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "test2", "test2", false);
        when(entityFinder.find(Policy.class, 2L)).thenReturn(policy2);
        when(rbacServices.isPermittedForEntity(user, policy, OperationType.UPDATE, null)).thenReturn(true);
        when(rbacServices.isPermittedForEntity(user, policy2, OperationType.UPDATE, null)).thenReturn(false);

        try {
            checker.checkBulkUpdatePermitted(user, null, EntityType.POLICY, oids);
            fail("Expected PermissionDeniedException");
        } catch (final PermissionDeniedException e) {
            assertEquals(OperationType.UPDATE, e.getOperation());
            assertEquals(policy2, e.getEntity());
            throw e;
        }
    }
}
