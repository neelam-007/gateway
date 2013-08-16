package com.l7tech.console.security.rbac;

import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PermissionSummaryPanelTest {
    private final Folder TEST_FOLDER = new Folder("test", null);
    @Mock
    private FolderAdmin folderAdmin;
    private PermissionsConfig config;
    private Set<SecurityZone> selectedZones;
    private Set<FolderHeader> selectedFolders;
    private Set<AttributePredicate> attributes;
    private Set<EntityHeader> entities;
    private Set<OperationType> operations;

    @Before
    public void setup() throws Exception {
        selectedZones = new HashSet<>();
        selectedFolders = new HashSet<>();
        attributes = new HashSet<>();
        entities = new HashSet<>();
        operations = new HashSet<>();
        config = new PermissionsConfig(new Role());
        config.setSelectedFolders(selectedFolders);
        config.setSelectedZones(selectedZones);
        config.setAttributePredicates(attributes);
        config.setSelectedEntities(entities);
        config.setOperations(operations);
        when(folderAdmin.findByPrimaryKey(Folder.DEFAULT_GOID)).thenReturn(TEST_FOLDER);
    }

    @Test
    public void generatePermissionsNoScope() {
        config.setScopeType(null);
        operations.add(OperationType.READ);
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);
        assertEquals(1, config.getGeneratedPermissions().size());
        assertTrue(config.getGeneratedPermissions().iterator().next().getScope().isEmpty());
    }

    @Test
    public void generatePermissionsNoScopeMultipleOps() {
        config.setScopeType(null);
        operations.add(OperationType.READ);
        operations.add(OperationType.CREATE);
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);
        assertEquals(2, config.getGeneratedPermissions().size());
        for (final Permission permission : config.getGeneratedPermissions()) {
            assertTrue(permission.getScope().isEmpty());
        }
    }

    @Test
    public void generatePermissionsZones() {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        // 2 zones
        final SecurityZone zone1 = new SecurityZone();
        zone1.setName("ZoneA");
        selectedZones.add(zone1);
        final SecurityZone zone2 = new SecurityZone();
        zone2.setName("ZoneB");
        selectedZones.add(zone2);
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(2, config.getGeneratedPermissions().size());
        for (final Permission permission : config.getGeneratedPermissions()) {
            final Map<Class, Integer> predicateTypes = countPredicateTypes(permission);
            assertEquals(new Integer(1), predicateTypes.get(SecurityZonePredicate.class));
        }
    }

    @Test
    public void generatePermissionsZonesMultipleOps() {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        operations.add(OperationType.UPDATE);
        // 2 zones
        final SecurityZone zone1 = new SecurityZone();
        zone1.setName("ZoneA");
        selectedZones.add(zone1);
        final SecurityZone zone2 = new SecurityZone();
        zone2.setName("ZoneB");
        selectedZones.add(zone2);
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(4, config.getGeneratedPermissions().size());
        for (final Permission permission : config.getGeneratedPermissions()) {
            final Map<Class, Integer> predicateTypes = countPredicateTypes(permission);
            assertEquals(new Integer(1), predicateTypes.get(SecurityZonePredicate.class));
        }
    }

    @BugId("SSG-6918")
    @Test
    public void generatePermissionsNullZone() {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        selectedZones.add(SecurityZoneUtil.getNullZone());
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(1, config.getGeneratedPermissions().size());
        final Permission permission = config.getGeneratedPermissions().iterator().next();
        assertEquals(1, permission.getScope().size());
        final SecurityZonePredicate zonePred = (SecurityZonePredicate) permission.getScope().iterator().next();
        assertNull(zonePred.getRequiredZone());
    }

    @Test
    public void generatePermissionsFolders() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        // 2 folders
        selectedFolders.add(new FolderHeader(TEST_FOLDER));
        final Folder folder2 = new Folder("test2", null);
        final Goid goid = new Goid(1, 2);
        folder2.setGoid(goid);
        selectedFolders.add(new FolderHeader(folder2));
        when(folderAdmin.findByPrimaryKey(goid)).thenReturn(folder2);
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(2, config.getGeneratedPermissions().size());
        for (final Permission permission : config.getGeneratedPermissions()) {
            final Map<Class, Integer> predicateTypes = countPredicateTypes(permission);
            assertEquals(new Integer(1), predicateTypes.get(FolderPredicate.class));
        }
    }

    @Test
    public void generatePermissionsFoldersMultipleOps() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        operations.add(OperationType.UPDATE);
        // 2 folders
        selectedFolders.add(new FolderHeader(TEST_FOLDER));
        final Folder folder2 = new Folder("test2", null);
        final Goid goid = new Goid(1, 2);
        folder2.setGoid(goid);
        selectedFolders.add(new FolderHeader(folder2));
        when(folderAdmin.findByPrimaryKey(goid)).thenReturn(folder2);
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(4, config.getGeneratedPermissions().size());
        final Set<OperationType> foundOps = new HashSet<>();
        for (final Permission permission : config.getGeneratedPermissions()) {
            foundOps.add(permission.getOperation());
            final Map<Class, Integer> predicateTypes = countPredicateTypes(permission);
            assertEquals(new Integer(1), predicateTypes.get(FolderPredicate.class));
        }
        assertEquals(2, foundOps.size());
        assertTrue(foundOps.contains(OperationType.READ));
        assertTrue(foundOps.contains(OperationType.UPDATE));
    }

    @Test
    public void generatePermissionsFoldersFindException() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        // 2 folders
        selectedFolders.add(new FolderHeader(TEST_FOLDER));
        final Folder folder2 = new Folder("test2", null);
        final Goid goid = new Goid(1, 2);
        folder2.setGoid(goid);
        selectedFolders.add(new FolderHeader(folder2));
        // should skip this folder
        when(folderAdmin.findByPrimaryKey(goid)).thenThrow(new FindException("mocking exception"));
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(1, config.getGeneratedPermissions().size());
        final Permission permission = config.getGeneratedPermissions().iterator().next();
        assertEquals(1, permission.getScope().size());
        final FolderPredicate folderPred = (FolderPredicate) permission.getScope().iterator().next();
        assertEquals(TEST_FOLDER, folderPred.getFolder());
    }

    @Test
    public void generatePermissionsFoldersTransitiveWithAncestry() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        config.setFolderAncestry(true);
        config.setFolderTransitive(true);
        // 2 folders
        selectedFolders.add(new FolderHeader(TEST_FOLDER));
        final Folder folder2 = new Folder("test2", null);
        final Goid goid = new Goid(1, 2);
        folder2.setGoid(goid);
        selectedFolders.add(new FolderHeader(folder2));
        when(folderAdmin.findByPrimaryKey(goid)).thenReturn(folder2);
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(8, config.getGeneratedPermissions().size());
        int ancestryPermissionCount = 0;
        int folderPermissionCount = 0;
        int specificFolderPermissionCount = 0;
        for (final Permission permission : config.getGeneratedPermissions()) {
            final Map<Class, Integer> predicateTypes = countPredicateTypes(permission);
            if (predicateTypes.containsKey(FolderPredicate.class)) {
                // regular folder permission
                assertEquals(new Integer(1), predicateTypes.get(FolderPredicate.class));
                folderPermissionCount++;
                assertTrue(((FolderPredicate) permission.getScope().iterator().next()).isTransitive());
            } else if (predicateTypes.containsKey(EntityFolderAncestryPredicate.class)) {
                // should be the ancestry permission
                assertEquals(new Integer(1), predicateTypes.get(EntityFolderAncestryPredicate.class));
                ancestryPermissionCount++;
            } else {
                // should be the read folder permission
                assertEquals(new Integer(1), predicateTypes.get(ObjectIdentityPredicate.class));
                specificFolderPermissionCount++;
            }
        }
        assertEquals(2, ancestryPermissionCount);
        assertEquals(4, folderPermissionCount);
        assertEquals(2, specificFolderPermissionCount);
    }

    @Test
    public void generatePermissionsFoldersTransitiveWithAncestryMultipleOps() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        operations.add(OperationType.UPDATE);
        config.setFolderAncestry(true);
        config.setFolderTransitive(true);
        // 2 folders
        selectedFolders.add(new FolderHeader(TEST_FOLDER));
        final Folder folder2 = new Folder("test2", null);
        final Goid goid = new Goid(1, 2);
        folder2.setGoid(goid);
        selectedFolders.add(new FolderHeader(folder2));
        when(folderAdmin.findByPrimaryKey(goid)).thenReturn(folder2);
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(10, config.getGeneratedPermissions().size());
        int ancestryPermissionCount = 0;
        int folderPermissionCount = 0;
        int specificFolderPermissionCount = 0;
        final Set<OperationType> foundOps = new HashSet<>();
        for (final Permission permission : config.getGeneratedPermissions()) {
            foundOps.add(permission.getOperation());
            final Map<Class, Integer> predicateTypes = countPredicateTypes(permission);
            if (predicateTypes.containsKey(FolderPredicate.class)) {
                // regular folder permission
                assertEquals(new Integer(1), predicateTypes.get(FolderPredicate.class));
                folderPermissionCount++;
                assertTrue(((FolderPredicate) permission.getScope().iterator().next()).isTransitive());
            } else if (predicateTypes.containsKey(EntityFolderAncestryPredicate.class)) {
                // should be the ancestry permission
                assertEquals(new Integer(1), predicateTypes.get(EntityFolderAncestryPredicate.class));
                ancestryPermissionCount++;
            } else {
                // should be the read folder permission
                assertEquals(new Integer(1), predicateTypes.get(ObjectIdentityPredicate.class));
                specificFolderPermissionCount++;
            }
        }
        assertEquals(2, ancestryPermissionCount);
        assertEquals(6, folderPermissionCount);
        assertEquals(2, specificFolderPermissionCount);
        assertEquals(2, foundOps.size());
        assertTrue(foundOps.contains(OperationType.READ));
        assertTrue(foundOps.contains(OperationType.UPDATE));
    }

    @Test
    public void generatePermissionsAttributes() {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        attributes.add(new AttributePredicate(null, "name", "test"));
        attributes.add(new AttributePredicate(null, "id", "1"));
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);
        assertEquals(1, config.getGeneratedPermissions().size());
        final Map<Class, Integer> predicateTypes = countPredicateTypes(config.getGeneratedPermissions().iterator().next());
        assertEquals(new Integer(2), predicateTypes.get(AttributePredicate.class));
    }

    @Test
    public void generatePermissionsAttributesMultipleOps() {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        operations.add(OperationType.UPDATE);
        attributes.add(new AttributePredicate(null, "name", "test"));
        attributes.add(new AttributePredicate(null, "id", "1"));
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(2, config.getGeneratedPermissions().size());
        final Set<OperationType> foundOps = new HashSet<>();
        for (final Permission permission : config.getGeneratedPermissions()) {
            foundOps.add(permission.getOperation());
            final Map<Class, Integer> predicateTypes = countPredicateTypes(permission);
            assertEquals(new Integer(2), predicateTypes.get(AttributePredicate.class));
        }
        assertEquals(2, foundOps.size());
        assertTrue(foundOps.contains(OperationType.READ));
        assertTrue(foundOps.contains(OperationType.UPDATE));
    }

    @Test
    public void generatePermissionsZoneAndFolderAndAttribute() {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        selectedZones.add(new SecurityZone());
        attributes.add(new AttributePredicate(null, "name", "test"));
        selectedFolders.add(new FolderHeader(TEST_FOLDER));
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(1, config.getGeneratedPermissions().size());
        final Permission permission = config.getGeneratedPermissions().iterator().next();
        assertEquals(3, permission.getScope().size());
        final Map<Class, Integer> predTypes = countPredicateTypes(permission);
        assertEquals(new Integer(1), predTypes.get(SecurityZonePredicate.class));
        assertEquals(new Integer(1), predTypes.get(FolderPredicate.class));
        assertEquals(new Integer(1), predTypes.get(AttributePredicate.class));
    }

    @Test
    public void generatePermissionsZoneAndFolderAndAttributeMultipleOps() {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        operations.add(OperationType.UPDATE);
        selectedZones.add(new SecurityZone());
        attributes.add(new AttributePredicate(null, "name", "test"));
        selectedFolders.add(new FolderHeader(TEST_FOLDER));
        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(2, config.getGeneratedPermissions().size());
        final Set<OperationType> foundOps = new HashSet<>();
        for (final Permission permission : config.getGeneratedPermissions()) {
            foundOps.add(permission.getOperation());
            assertEquals(3, permission.getScope().size());
            final Map<Class, Integer> predTypes = countPredicateTypes(permission);
            assertEquals(new Integer(1), predTypes.get(SecurityZonePredicate.class));
            assertEquals(new Integer(1), predTypes.get(FolderPredicate.class));
            assertEquals(new Integer(1), predTypes.get(AttributePredicate.class));
        }
        assertEquals(2, foundOps.size());
        assertTrue(foundOps.contains(OperationType.READ));
        assertTrue(foundOps.contains(OperationType.UPDATE));
    }

    @Test
    public void generatePermissionsMultipleZoneAndFolderAndAttribute() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        // 2 zones
        final SecurityZone zone1 = new SecurityZone();
        zone1.setName("ZoneA");
        selectedZones.add(zone1);
        final SecurityZone zone2 = new SecurityZone();
        zone2.setName("ZoneB");
        selectedZones.add(zone2);

        // 2 attributes
        attributes.add(new AttributePredicate(null, "name", "test"));
        attributes.add(new AttributePredicate(null, "id", "1"));

        // 2 folders
        selectedFolders.add(new FolderHeader(TEST_FOLDER));
        final Folder folder2 = new Folder("test2", null);
        final Goid goid = new Goid(1, 2);
        folder2.setGoid(goid);
        when(folderAdmin.findByPrimaryKey(goid)).thenReturn(folder2);
        selectedFolders.add(new FolderHeader(folder2));

        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(4, config.getGeneratedPermissions().size());
        for (final Permission permission : config.getGeneratedPermissions()) {
            assertEquals(4, permission.getScope().size());
            final Map<Class, Integer> predicateTypes = countPredicateTypes(permission);
            assertEquals(new Integer(1), predicateTypes.get(SecurityZonePredicate.class));
            assertEquals(new Integer(1), predicateTypes.get(FolderPredicate.class));
            assertEquals(new Integer(2), predicateTypes.get(AttributePredicate.class));
        }
    }

    @Test
    public void generatePermissionsMultipleZoneAndFolderAndAttributeMultipleOps() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.CONDITIONAL);
        operations.add(OperationType.READ);
        operations.add(OperationType.UPDATE);
        // 2 zones
        final SecurityZone zone1 = new SecurityZone();
        zone1.setName("ZoneA");
        selectedZones.add(zone1);
        final SecurityZone zone2 = new SecurityZone();
        zone2.setName("ZoneB");
        selectedZones.add(zone2);

        // 2 attributes
        attributes.add(new AttributePredicate(null, "name", "test"));
        attributes.add(new AttributePredicate(null, "id", "1"));

        // 2 folders
        selectedFolders.add(new FolderHeader(TEST_FOLDER));
        final Folder folder2 = new Folder("test2", null);
        final Goid goid = new Goid(1, 2);
        folder2.setGoid(goid);
        when(folderAdmin.findByPrimaryKey(goid)).thenReturn(folder2);
        selectedFolders.add(new FolderHeader(folder2));

        PermissionSummaryPanel.generatePermissions(config, folderAdmin);

        assertEquals(8, config.getGeneratedPermissions().size());
        final Set<OperationType> foundOps = new HashSet<>();
        for (final Permission permission : config.getGeneratedPermissions()) {
            foundOps.add(permission.getOperation());
            assertEquals(4, permission.getScope().size());
            final Map<Class, Integer> predicateTypes = countPredicateTypes(permission);
            assertEquals(new Integer(1), predicateTypes.get(SecurityZonePredicate.class));
            assertEquals(new Integer(1), predicateTypes.get(FolderPredicate.class));
            assertEquals(new Integer(2), predicateTypes.get(AttributePredicate.class));
        }
        assertEquals(2, foundOps.size());
        assertTrue(foundOps.contains(OperationType.READ));
        assertTrue(foundOps.contains(OperationType.UPDATE));
    }

    @Test
    public void generatePermissionsSpecificAssertions() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.SPECIFIC_OBJECTS);
        config.setType(EntityType.ASSERTION_ACCESS);
        operations.add(OperationType.READ);
        entities.add(new EntityHeader("1", EntityType.ASSERTION_ACCESS, AllAssertion.class.getName(), null));

        PermissionSummaryPanel.generatePermissions(config, folderAdmin);
        assertEquals(1, config.getGeneratedPermissions().size());
        final Map<Class, Integer> predTypes = countPredicateTypes(config.getGeneratedPermissions().iterator().next());
        assertEquals(new Integer(1), predTypes.get(AttributePredicate.class));
    }

    @Test
    public void generatePermissionsMultipleSpecificAssertionsAndOps() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.SPECIFIC_OBJECTS);
        config.setType(EntityType.ASSERTION_ACCESS);
        operations.add(OperationType.READ);
        operations.add(OperationType.UPDATE);
        entities.add(new EntityHeader("1", EntityType.ASSERTION_ACCESS, AllAssertion.class.getName(), null));
        entities.add(new EntityHeader("2", EntityType.ASSERTION_ACCESS, Include.class.getName(), null));

        PermissionSummaryPanel.generatePermissions(config, folderAdmin);
        assertEquals(4, config.getGeneratedPermissions().size());
        final Set<OperationType> foundOps = new HashSet<>();
        for (final Permission permission : config.getGeneratedPermissions()) {
            foundOps.add(permission.getOperation());
            assertEquals(1, permission.getScope().size());
            assertTrue(permission.getScope().iterator().next() instanceof AttributePredicate);
        }
        assertEquals(2, foundOps.size());
        assertTrue(foundOps.contains(OperationType.READ));
        assertTrue(foundOps.contains(OperationType.UPDATE));
    }

    @Test
    public void generatePermissionsSpecificFolder() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.SPECIFIC_OBJECTS);
        config.setType(EntityType.FOLDER);
        config.setSpecificFolderAncestry(false);
        operations.add(OperationType.READ);
        entities.add(new EntityHeader("1", EntityType.FOLDER, "test", null));

        PermissionSummaryPanel.generatePermissions(config, folderAdmin);
        assertEquals(1, config.getGeneratedPermissions().size());
        final Map<Class, Integer> predTypes = countPredicateTypes(config.getGeneratedPermissions().iterator().next());
        assertEquals(new Integer(1), predTypes.get(ObjectIdentityPredicate.class));
    }

    @Test
    public void generatePermissionsSpecificFolderWithAncestry() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.SPECIFIC_OBJECTS);
        config.setType(EntityType.FOLDER);
        config.setSpecificFolderAncestry(true);
        operations.add(OperationType.READ);
        entities.add(new EntityHeader("1", EntityType.FOLDER, "test", null));

        PermissionSummaryPanel.generatePermissions(config, folderAdmin);
        assertEquals(2, config.getGeneratedPermissions().size());
        final Map<Class, Integer> predTypes = countPredicateTypes(config.getGeneratedPermissions());
        assertEquals(new Integer(1), predTypes.get(ObjectIdentityPredicate.class));
        assertEquals(new Integer(1), predTypes.get(EntityFolderAncestryPredicate.class));
    }

    @Test
    public void generatePermissionsMultipleSpecificFolderAndOpsWithAncestry() throws Exception {
        config.setScopeType(PermissionsConfig.ScopeType.SPECIFIC_OBJECTS);
        config.setType(EntityType.FOLDER);
        config.setSpecificFolderAncestry(true);
        operations.add(OperationType.READ);
        operations.add(OperationType.UPDATE);
        entities.add(new EntityHeader(new Goid(0, 1), EntityType.FOLDER, "test", null));
        entities.add(new EntityHeader(new Goid(0, 2), EntityType.FOLDER, "test2", null));

        PermissionSummaryPanel.generatePermissions(config, folderAdmin);
        assertEquals(6, config.getGeneratedPermissions().size());
        final Map<Class, Integer> predTypes = countPredicateTypes(config.getGeneratedPermissions());
        assertEquals(new Integer(4), predTypes.get(ObjectIdentityPredicate.class));
        assertEquals(new Integer(2), predTypes.get(EntityFolderAncestryPredicate.class));
    }

    private Map<Class, Integer> countPredicateTypes(final Permission permission) {
        return countPredicateTypes(Collections.singleton(permission));
    }

    private Map<Class, Integer> countPredicateTypes(final Collection<Permission> permissions) {
        final Map<Class, Integer> predicateTypes = new HashMap<>();
        for (final Permission permission : permissions) {
            for (final ScopePredicate predicate : permission.getScope()) {
                final Class<? extends ScopePredicate> predClass = predicate.getClass();
                if (!predicateTypes.containsKey(predClass)) {
                    predicateTypes.put(predClass, 1);
                } else {
                    predicateTypes.put(predClass, predicateTypes.get(predClass) + 1);
                }
            }
        }
        return predicateTypes;
    }

}
