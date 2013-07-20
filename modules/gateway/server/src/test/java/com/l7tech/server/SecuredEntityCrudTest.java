package com.l7tech.server;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
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

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SecuredEntityCrudTest {
    private static final Goid ZONE_GOID = new Goid(0,1234L);
    private SecuredEntityCrud securedEntityCrud;
    @Mock
    private RbacServices rbacServices;
    @Mock
    private SecurityFilter securityFilter;
    @Mock
    private EntityCrud entityCrud;
    @Mock
    private ZoneUpdateSecurityChecker zoneUpdateSecurityChecker;
    private List<Serializable> oids;
    private Map<EntityType, Collection<Serializable>> oidsMap;
    private List<EntityHeader> entities;

    @Before
    public void setup() {
        securedEntityCrud = new SecuredEntityCrud(rbacServices, securityFilter, entityCrud, zoneUpdateSecurityChecker);
        oids = new ArrayList<>();
        oidsMap = new HashMap<>();
        entities = new ArrayList<>();
    }

    @Test
    public void findByEntityTypeAndSecurityZoneOidFiltersEntities() throws Exception {
        entities.add(new ZoneableEntityHeader());
        entities.add(new ZoneableEntityHeader());
        when(entityCrud.findByEntityTypeAndSecurityZoneGoid(EntityType.POLICY, ZONE_GOID)).thenReturn(entities);
        when(securityFilter.filter(eq(entities), any(User.class), eq(OperationType.READ), eq((String) null))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final List<ZoneableEntityHeader> arg = (List<ZoneableEntityHeader>) invocationOnMock.getArguments()[0];
                assertEquals(2, arg.size());
                arg.remove(0);
                return arg;
            }
        });
        final Collection<EntityHeader> filtered = securedEntityCrud.findByEntityTypeAndSecurityZoneGoid(EntityType.POLICY, ZONE_GOID);
        assertEquals(1, filtered.size());
    }

    @Test
    public void setSecurityZoneForEntities() throws Exception {
        securedEntityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, oids);
        verify(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), eq(ZONE_GOID), eq(EntityType.POLICY), eq(oids));
        verify(entityCrud).setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, oids);
    }

    @Test(expected = PermissionDeniedException.class)
    public void setSecurityZoneForEntitiesPermissionDenied() throws Exception {
        doThrow(new PermissionDeniedException(OperationType.UPDATE, EntityType.POLICY, "mocking exception")).when(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), eq(ZONE_GOID), eq(EntityType.POLICY), eq(oids));
        try {
            securedEntityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, oids);
            fail("Expected PermissionDeniedException");
        } catch (final PermissionDeniedException e) {
            verify(entityCrud, never()).setSecurityZoneForEntities(any(Goid.class), any(EntityType.class), any(Collection.class));
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesFindException() throws Exception {
        doThrow(new FindException("mocking exception")).when(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), any(Goid.class), any(EntityType.class), anyCollection());
        try {
            securedEntityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, oids);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(entityCrud, never()).setSecurityZoneForEntities(any(Goid.class), any(EntityType.class), anyCollection());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesUpdateException() throws Exception {
        doThrow(new UpdateException("mocking exception")).when(entityCrud).setSecurityZoneForEntities(any(Goid.class), any(EntityType.class), anyCollection());
        try {
            securedEntityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, oids);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), eq(ZONE_GOID), eq(EntityType.POLICY), eq(oids));
            throw e;
        }
    }

    @Test
    public void setSecurityZoneForEntitiesMultipleEntityTypes() throws Exception {
        securedEntityCrud.setSecurityZoneForEntities(ZONE_GOID, oidsMap);
        verify(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), eq(ZONE_GOID), eq(oidsMap));
        verify(entityCrud).setSecurityZoneForEntities(ZONE_GOID, oidsMap);
    }

    @Test(expected = PermissionDeniedException.class)
    public void setSecurityZoneForEntitiesMultipleEntityTypesPermissionDenied() throws Exception {
        doThrow(new PermissionDeniedException(OperationType.UPDATE, EntityType.POLICY, "mocking exception")).when(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), eq(ZONE_GOID), eq(oidsMap));
        try {
            securedEntityCrud.setSecurityZoneForEntities(ZONE_GOID, oidsMap);
            fail("Expected PermissionDeniedException");
        } catch (final PermissionDeniedException e) {
            verify(entityCrud, never()).setSecurityZoneForEntities(any(Goid.class), any(EntityType.class), any(Collection.class));
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesMultipleEntityTypesFindException() throws Exception {
        doThrow(new FindException("mocking exception")).when(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), any(Goid.class), anyMap());
        try {
            securedEntityCrud.setSecurityZoneForEntities(ZONE_GOID, oidsMap);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(entityCrud, never()).setSecurityZoneForEntities(any(Goid.class), anyMap());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesMultipleEntityTypesUpdateException() throws Exception {
        doThrow(new UpdateException("mocking exception")).when(entityCrud).setSecurityZoneForEntities(any(Goid.class), anyMap());
        try {
            securedEntityCrud.setSecurityZoneForEntities(ZONE_GOID, oidsMap);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            verify(zoneUpdateSecurityChecker).checkBulkUpdatePermitted(any(User.class), eq(ZONE_GOID), eq(oidsMap));
            throw e;
        }
    }
}
