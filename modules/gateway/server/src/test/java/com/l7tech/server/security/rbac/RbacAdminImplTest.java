package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RbacAdminImplTest {
    private static final Goid ZONE_GOID = new Goid(0, 1234L);
    private static final Goid GOID = new Goid(0, 5678L);
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
        zone = createSecurityZone(ZONE_GOID);
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
        when(securityZoneManager.findByPrimaryKey(ZONE_GOID)).thenReturn(zone);
        assertEquals(zone, admin.findSecurityZoneByPrimaryKey(ZONE_GOID));
    }

    @Test
    public void saveExistingSecurityZone() throws Exception {
        final Goid goid = admin.saveSecurityZone(zone);
        assertEquals(ZONE_GOID, goid);
        verify(securityZoneManager).update(zone);
        verify(securityZoneManager).updateRoles(zone);
    }

    @Test
    public void saveNewSecurityZone() throws Exception {
        final SecurityZone newZone = createSecurityZone(SecurityZone.DEFAULT_GOID);
        when(securityZoneManager.save(newZone)).thenReturn(ZONE_GOID);
        final Goid goid = admin.saveSecurityZone(newZone);
        assertEquals(ZONE_GOID, goid);
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
        verify(securityZoneManager).delete(zone);
    }

    @Test
    public void findRolesForUserAttachesEntities() throws Exception {
        final Role role = new Role();
        role.setName("Manage Test Service (#1234)");
        role.setEntityType(EntityType.SERVICE);
        role.setEntityGoid(GOID);
        final User user = new InternalUser("test");
        final PublishedService service = new PublishedService();
        when(roleManager.getAssignedRoles(user, true, false)).thenReturn(Arrays.asList(role));
        when(entityCrud.find(EntityType.SERVICE.getEntityClass(), GOID)).thenReturn(service);

        final Collection<Role> assignedRoles = admin.findRolesForUser(user);
        assertEquals(1, assignedRoles.size());
        final Role assignedRole = assignedRoles.iterator().next();
        assertEquals(service, assignedRole.getCachedSpecificEntity());
    }

    @Test
    public void findRolesForUserAttachesGoidEntities() throws Exception {
        final Role role = new Role();
        role.setName("Manage Test Service (#1234)");
        role.setEntityType(EntityType.JDBC_CONNECTION);
        role.setEntityGoid(ZONE_GOID);
        final User user = new InternalUser("test");
        final JdbcConnection connection = new JdbcConnection();
        when(roleManager.getAssignedRoles(user, true, false)).thenReturn(Arrays.asList(role));
        when(entityCrud.find(EntityType.JDBC_CONNECTION.getEntityClass(), ZONE_GOID)).thenReturn(connection);

        final Collection<Role> assignedRoles = admin.findRolesForUser(user);
        assertEquals(1, assignedRoles.size());
        final Role assignedRole = assignedRoles.iterator().next();
        assertEquals(connection, assignedRole.getCachedSpecificEntity());
    }

    @Test
    public void findRolesForGroup() throws Exception {
        final Role role = new Role();
        role.setName("Manage Test Service (#1234)");
        role.setEntityType(EntityType.JDBC_CONNECTION);
        role.setEntityGoid(GOID);
        final Group group = new InternalGroup("test");
        final JdbcConnection connection = new JdbcConnection();
        when(roleManager.getAssignedRoles(group)).thenReturn(Arrays.asList(role));
        when(entityCrud.find(EntityType.JDBC_CONNECTION.getEntityClass(), GOID)).thenReturn(connection);

        final Collection<Role> assignedRoles = admin.findRolesForGroup(group);
        assertEquals(1, assignedRoles.size());
        final Role assignedRole = assignedRoles.iterator().next();
        assertEquals(connection, assignedRole.getCachedSpecificEntity());
    }

    private SecurityZone createSecurityZone(final Goid goid) {
        final SecurityZone zone = new SecurityZone();
        zone.setGoid(goid);
        return zone;
    }
}
