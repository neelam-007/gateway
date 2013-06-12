package com.l7tech.server;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.ZoneUpdateSecurityChecker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SecuredEntityCrudTest {
    private static final long ZONE_OID = 1234L;
    private SecuredEntityCrud securedEntityCrud;
    @Mock
    private RbacServices rbacServices;
    @Mock
    private SecurityFilter securityFilter;
    @Mock
    private EntityCrud entityCrud;
    @Mock
    private ZoneUpdateSecurityChecker zoneUpdateSecurityChecker;
    private List<Long> oids;
    private List<EntityHeader> entities;

    @Before
    public void setup() {
        securedEntityCrud = new SecuredEntityCrud(rbacServices, securityFilter, entityCrud, zoneUpdateSecurityChecker);
        oids = new ArrayList<>();
        entities = new ArrayList<>();
    }

    @Test
    public void findByEntityTypeAndSecurityZoneOidFiltersEntities() throws Exception {
        entities.add(new ZoneableEntityHeader());
        entities.add(new ZoneableEntityHeader());
        when(entityCrud.findByEntityTypeAndSecurityZoneOid(EntityType.POLICY, ZONE_OID)).thenReturn(entities);
        when(securityFilter.filter(eq(entities), any(User.class), eq(OperationType.READ), eq((String) null))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final List<ZoneableEntityHeader> arg = (List<ZoneableEntityHeader>) invocationOnMock.getArguments()[0];
                assertEquals(2, arg.size());
                arg.remove(0);
                return arg;
            }
        });
        final Collection<EntityHeader> filtered = securedEntityCrud.findByEntityTypeAndSecurityZoneOid(EntityType.POLICY, ZONE_OID);
        assertEquals(1, filtered.size());
    }

    @Test
    public void setSecurityZoneForEntities() throws Exception {
        securedEntityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
        verify(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), eq(ZONE_OID), eq(EntityType.POLICY), eq(oids));
        verify(entityCrud).setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
    }

    @Test(expected = PermissionDeniedException.class)
    public void setSecurityZoneForEntitiesPermissionDenied() throws Exception {
        doThrow(new PermissionDeniedException(OperationType.UPDATE, EntityType.POLICY, "mocking exception")).when(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), eq(ZONE_OID), eq(EntityType.POLICY), eq(oids));
        try {
            securedEntityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
            fail("Expected PermissionDeniedException");
        } catch (final PermissionDeniedException e) {
            verify(entityCrud, never()).setSecurityZoneForEntities(anyLong(), any(EntityType.class), any(Collection.class));
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesFindException() throws Exception {
        doThrow(new FindException("mocking exception")).when(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), anyLong(), any(EntityType.class), anyCollection());
        try {
            securedEntityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(entityCrud, never()).setSecurityZoneForEntities(anyLong(), any(EntityType.class), anyCollection());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesUpdateException() throws Exception {
        doThrow(new UpdateException("mocking exception")).when(entityCrud).setSecurityZoneForEntities(anyLong(), any(EntityType.class), anyCollection());
        try {
            securedEntityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), eq(ZONE_OID), eq(EntityType.POLICY), eq(oids));
            throw e;
        }
    }
}
