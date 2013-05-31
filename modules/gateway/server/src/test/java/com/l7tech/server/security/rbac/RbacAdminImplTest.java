package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.policy.AssertionAccessManager;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RbacAdminImplTest {
    private static final long OID = 1234L;
    private RbacAdminImpl admin;
    @Mock
    private RoleManager roleManager;
    @Mock
    private EntityCrud entityCrud;
    @Mock
    private SecurityZoneManager securityZoneManager;
    @Mock
    private AssertionAccessManager assertionAccessManager;
    @Mock
    private AssertionRegistry assertionRegistry;
    private List<SecurityZone> zones;
    private SecurityZone zone;

    @Before
    public void setup() {
        admin = new RbacAdminImpl(roleManager);
        zone = createSecurityZone(OID);
        zones = new ArrayList<SecurityZone>();
        zones.add(zone);
        ApplicationContexts.inject(admin, CollectionUtils.<String, Object>mapBuilder()
                .put("securityZoneManager", securityZoneManager)
                .put("assertionRegistry", assertionRegistry)
                .put("assertionAccessManager", assertionAccessManager)
                .put("entityCrud", entityCrud).unmodifiableMap());
    }

    @Test
    public void findAllSecurityZones() throws Exception {
        when(securityZoneManager.findAll()).thenReturn(zones);
        final Collection<SecurityZone> found = admin.findAllSecurityZones();
        assertEquals(1, found.size());
        assertEquals(zone, found.iterator().next());
    }

    @Test
    public void findSecurityZoneByPrimaryKey() throws Exception {
        when(securityZoneManager.findByPrimaryKey(OID)).thenReturn(zone);
        assertEquals(zone, admin.findSecurityZoneByPrimaryKey(OID));
    }

    @Test
    public void saveExistingSecurityZone() throws Exception {
        final long oid = admin.saveSecurityZone(zone);
        assertEquals(OID, oid);
        verify(securityZoneManager).update(zone);
        verify(securityZoneManager).updateRoles(zone);
    }

    @Test
    public void saveNewSecurityZone() throws Exception {
        final SecurityZone newZone = createSecurityZone(SecurityZone.DEFAULT_OID);
        when(securityZoneManager.save(newZone)).thenReturn(OID);
        final long oid = admin.saveSecurityZone(newZone);
        assertEquals(OID, oid);
        verify(securityZoneManager).save(newZone);
        verify(securityZoneManager).createRoles(newZone);
    }

    @Test(expected = SaveException.class)
    public void saveExistingSecurityZoneUpdateError() throws Exception {
        doThrow(new UpdateException("mocking exception")).when(securityZoneManager).update(zone);
        admin.saveSecurityZone(zone);
    }

    @Test
    public void deleteSecurityZone() throws Exception {
        admin.deleteSecurityZone(zone);
        verify(securityZoneManager).deleteRoles(OID);
        verify(securityZoneManager).delete(zone);
    }

    private SecurityZone createSecurityZone(final long oid) {
        final SecurityZone zone = new SecurityZone();
        zone.setOid(oid);
        return zone;
    }
}
