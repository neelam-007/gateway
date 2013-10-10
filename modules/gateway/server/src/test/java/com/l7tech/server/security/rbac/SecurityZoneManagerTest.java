package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityManagerTest;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SecurityZoneManagerTest extends EntityManagerTest {
    private SecurityZoneManager zoneManager;
    private RoleManager roleManager;
    private Goid zoneGoid;
    private SecurityZone persistedZone;

    @Before
    public void setup() throws SaveException {
        zoneManager = applicationContext.getBean("securityZoneManager", SecurityZoneManager.class);
        roleManager = applicationContext.getBean("roleManager", RoleManager.class);
        persistedZone = new SecurityZone();
        persistedZone.setName("Test");
        persistedZone.setDescription("Test Zone");
        persistedZone.setPermittedEntityTypes(Collections.singleton(EntityType.ANY));
        zoneGoid = zoneManager.save(persistedZone);
        session.flush();
    }

    @Test
    public void getImpClass() {
        assertEquals(SecurityZone.class, zoneManager.getImpClass());
    }

    @Test
    public void findByPrimaryKey() throws FindException {
        assertEqualToPersistedZone(zoneManager.findByPrimaryKey(zoneGoid));
    }

    @Test
    public void findByHeader() throws FindException {
        final EntityHeader header = new EntityHeader();
        header.setGoid(zoneGoid);
        assertEqualToPersistedZone(zoneManager.findByHeader(header));
    }

    @Test
    public void save() throws Exception {
        final SecurityZone toSave = new SecurityZone();
        toSave.setName("New Zone");
        toSave.setDescription("New Zone Description");
        toSave.setPermittedEntityTypes(new HashSet<>(Arrays.asList(EntityType.SERVICE, EntityType.POLICY)));
        final Goid savedGoid = zoneManager.save(toSave);
        session.flush();
        assertNotNull(savedGoid);
        assertEquals(toSave, zoneManager.findByPrimaryKey(savedGoid));
    }

    @Test
    public void saveAlreadyExists() throws Exception {
        persistedZone.setName("Test Updated");
        final Goid savedGoid = zoneManager.save(persistedZone);
        session.flush();
        // make sure the goid hasn't changed
        assertEquals(zoneGoid, savedGoid);
    }

    @Test
    public void update() throws Exception {
        persistedZone.setName("Updated");
        zoneManager.update(persistedZone);
        session.flush();

        final SecurityZone found = zoneManager.findByPrimaryKey(zoneGoid);
        assertEquals("Updated", found.getName());
    }

    @Test
    public void updateNotFound() throws Exception {
        final SecurityZone doesNotExist = new SecurityZone();
        doesNotExist.setName("Does Not Exist");
        zoneManager.update(doesNotExist);
        session.flush();

        final SecurityZone found = zoneManager.findByUniqueName("Does Not Exist");
        // Updating a non-existing entity actually persists the entity!
        // TODO is this what we want??
        assertNotNull(found);
    }

    @Test
    public void getVersion() throws Exception {
        assertEquals(new Integer(0), zoneManager.getVersion(zoneGoid));
    }

    @Test
    public void getVersionNotFound() throws Exception {
        assertNull(zoneManager.getVersion(new Goid(1, 2)));
    }

    @Test
    public void getVersionMap() throws Exception {
        final Map<Goid, Integer> versionMap = zoneManager.findVersionMap();
        assertEquals(1, versionMap.size());
        assertEquals(new Integer(0), versionMap.get(zoneGoid));
    }

    @Test
    public void getEntityType() throws Exception {
        assertEquals(EntityType.SECURITY_ZONE, zoneManager.getEntityType());
    }

    @Test
    public void findAllHeaders() throws Exception {
        final Collection<EntityHeader> headers = zoneManager.findAllHeaders();
        assertEquals(1, headers.size());
        final EntityHeader header = headers.iterator().next();
        assertEquals(zoneGoid, header.getGoid());
        assertEquals(EntityType.SECURITY_ZONE, header.getType());
        assertEquals("Test", header.getName());
        assertEquals(StringUtils.EMPTY, header.getDescription());
        assertEquals(new Integer(0), header.getVersion());
    }

    @Test
    public void findAll() throws Exception {
        final Collection<SecurityZone> all = zoneManager.findAll();
        assertEquals(1, all.size());
        assertEqualToPersistedZone(all.iterator().next());
    }

    @Test
    public void deleteByGoid() throws Exception {
        zoneManager.delete(zoneGoid);
        session.flush();
        assertTrue(zoneManager.findAll().isEmpty());
    }

    @Test
    public void deleteByGoidNotFound() throws Exception {
        zoneManager.delete(new Goid(1, 2));
        session.flush();
        // initial zone should still be there
        assertEquals(1, zoneManager.findAll().size());
    }

    @Test
    public void delete() throws Exception {
        zoneManager.delete(persistedZone);
        session.flush();
        assertTrue(zoneManager.findAll().isEmpty());
    }

    @Test
    public void deleteNotFound() throws Exception {
        zoneManager.delete(new SecurityZone());
        session.flush();
        // initial zone should still be there
        assertEquals(1, zoneManager.findAll().size());
    }

    @Test
    public void getTableName() {
        assertEquals("security_zone", zoneManager.getTableName());
    }

    @Test
    public void findByUniqueName() throws Exception {
        assertEqualToPersistedZone(zoneManager.findByUniqueName("Test"));
    }

    @Test
    public void findByUniqueNameNotFound() throws Exception {
        assertNull(zoneManager.findByUniqueName("Does Not Exist"));
    }

    @Test
    public void createRoles() throws Exception {
        zoneManager.createRoles(persistedZone);
        session.flush();

        final Role viewZoneRole = roleManager.findByUniqueName("View Test Zone (#" + zoneGoid.toHexString() + ")");
        assertEquals(3, viewZoneRole.getPermissions().size());
        final Map<EntityType, List<Permission>> viewTypes = groupPermissionsByType(viewZoneRole);
        assertEquals(1, viewTypes.get(EntityType.FOLDER).size());
        assertEquals(1, viewTypes.get(EntityType.SECURITY_ZONE).size());
        assertEquals(1, viewTypes.get(EntityType.ANY).size());
        final Map<OperationType, List<Permission>> viewOps = groupPermissionsByOperation(viewZoneRole);
        assertEquals(3, viewOps.get(OperationType.READ).size());

        final Role manageZoneRole = roleManager.findByUniqueName("Manage Test Zone (#" + zoneGoid.toHexString() + ")");
        final Map<EntityType, List<Permission>> manageTypes = groupPermissionsByType(manageZoneRole);
        assertEquals(1, manageTypes.get(EntityType.FOLDER).size());
        assertEquals(1, manageTypes.get(EntityType.SECURITY_ZONE).size());
        assertEquals(4, manageTypes.get(EntityType.ANY).size());
        final Map<OperationType, List<Permission>> manageOps = groupPermissionsByOperation(manageZoneRole);
        assertEquals(3, manageOps.get(OperationType.READ).size());
        assertEquals(1, manageOps.get(OperationType.CREATE).size());
        assertEquals(1, manageOps.get(OperationType.UPDATE).size());
        assertEquals(1, manageOps.get(OperationType.DELETE).size());
    }

    @Test
    public void updateRoles() throws Exception {
        zoneManager.createRoles(persistedZone);
        session.flush();
        persistedZone.setName("Updated");
        zoneManager.updateRoles(persistedZone);
        session.flush();

        assertNull(roleManager.findByUniqueName("View Test Zone (#" + zoneGoid.toHexString() + ")"));
        assertNull(roleManager.findByUniqueName("Manage Test Zone (#" + zoneGoid.toHexString() + ")"));

        final Role viewZoneRole = roleManager.findByUniqueName("View Updated Zone (#" + zoneGoid.toHexString() + ")");
        assertNotNull(viewZoneRole);
        final Role manageZoneRole = roleManager.findByUniqueName("Manage Updated Zone (#" + zoneGoid.toHexString() + ")");
        assertNotNull(manageZoneRole);
    }

    private void assertEqualToPersistedZone(final SecurityZone found) {
        assertEquals(zoneGoid, found.getGoid());
        assertEquals("Test", found.getName());
        assertEquals("Test Zone", found.getDescription());
        assertEquals(1, found.getPermittedEntityTypes().size());
        assertEquals(EntityType.ANY, found.getPermittedEntityTypes().iterator().next());
    }

    private Map<EntityType, List<Permission>> groupPermissionsByType(final Role role) {
        final Map<EntityType, List<Permission>> permissionsByType = new HashMap<>();
        for (final Permission permission : role.getPermissions()) {
            if (!permissionsByType.containsKey(permission.getEntityType())) {
                permissionsByType.put(permission.getEntityType(), new ArrayList<Permission>());
            }
            permissionsByType.get(permission.getEntityType()).add(permission);
        }
        return permissionsByType;
    }

    private Map<OperationType, List<Permission>> groupPermissionsByOperation(final Role role) {
        final Map<OperationType, List<Permission>> permissionsByType = new HashMap<>();
        for (final Permission permission : role.getPermissions()) {
            if (!permissionsByType.containsKey(permission.getOperation())) {
                permissionsByType.put(permission.getOperation(), new ArrayList<Permission>());
            }
            permissionsByType.get(permission.getOperation()).add(permission);
        }
        return permissionsByType;
    }
}
