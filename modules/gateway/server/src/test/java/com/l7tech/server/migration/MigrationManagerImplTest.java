package com.l7tech.server.migration;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.cluster.ExternalEntityHeaderEnhancer;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.bundle.ExportedItem;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MigrationManagerImplTest {
    private MigrationManagerImpl manager;
    @Mock
    private EntityCrud entityCrud;
    @Mock
    private PropertyResolverFactory factory;
    @Mock
    private ExternalEntityHeaderEnhancer headerEnhancer;
    @Mock
    private PropertyResolver propertyResolver;

    @Before
    public void setup() {
        manager = new MigrationManagerImpl(entityCrud, factory, headerEnhancer);
    }

    /**
     * Ensure that the target SSG can handle a bundle which contains a service that has an Encapsulated Assertion dependency.
     *
     * The Encapsulated Assertion is expected to already exist on the target SSG.
     */
    @BugId("EM-914")
    @Test
    public void importBundleWithServiceThatUsesEncapsulatedAssertion() throws Exception {
        final MigrationBundle bundle = createServiceMigrationBundleWithEncassDependency();
        final PublishedService serviceToMigrate = (PublishedService) bundle.getExportedEntities().values().iterator().next();

        when(entityCrud.find(argThat(hasEntityType(EntityType.FOLDER)))).thenReturn(new Folder("RootFolder", null));
        // encass exists on target
        when(entityCrud.find(argThat(hasEntityType(EntityType.ENCAPSULATED_ASSERTION)))).thenReturn(new EncapsulatedAssertionConfig());
        // service doesn't exist on target
        when(entityCrud.find(argThat(hasEntityType(EntityType.SERVICE)))).thenReturn(null);

        when(factory.getPropertyResolver(any(PropertyResolver.Type.class))).thenReturn(propertyResolver);
        when(entityCrud.save(serviceToMigrate)).thenReturn(new Goid(0,1234L));

        final Collection<MigratedItem> migratedItems = manager.importBundle(bundle, false);
        verify(entityCrud).save(serviceToMigrate);
        assertEquals(2, migratedItems.size());
        final Map<EntityType, MigratedItem> migratedItemsByType = new HashMap<EntityType, MigratedItem>();
        for (final MigratedItem migratedItem : migratedItems) {
            migratedItemsByType.put(migratedItem.getTargetHeader().getType(), migratedItem);
        }
        assertTrue(migratedItemsByType.containsKey(EntityType.SERVICE));
        assertTrue(migratedItemsByType.containsKey(EntityType.ENCAPSULATED_ASSERTION));
        assertEquals(MigratedItem.ImportOperation.CREATE, migratedItemsByType.get(EntityType.SERVICE).getOperation());
        // encass dependency should have been mapped to an existing encass on the target ssg
        assertEquals(MigratedItem.ImportOperation.MAP_EXISTING, migratedItemsByType.get(EntityType.ENCAPSULATED_ASSERTION).getOperation());
    }

    /**
     * Should return an error message if the bundle contains a service which depends on an Encapsulated Assertion that does not exist on the target SSG.
     */
    @BugId("EM-914")
    @Test(expected = MigrationApi.MigrationException.class)
    public void importBundleWithServiceThatUsesEncapsulatedAssertionWhichDoesNotExistOnTarget() throws Exception {
        final MigrationBundle bundle = createServiceMigrationBundleWithEncassDependency();
        final PublishedService serviceToMigrate = (PublishedService) bundle.getExportedEntities().values().iterator().next();

        when(entityCrud.find(argThat(hasEntityType(EntityType.FOLDER)))).thenReturn(new Folder("RootFolder", null));
        // encass does NOT exist on target
        when(entityCrud.find(argThat(hasEntityType(EntityType.ENCAPSULATED_ASSERTION)))).thenReturn(null);
        // service doesn't exist on target
        when(entityCrud.find(argThat(hasEntityType(EntityType.SERVICE)))).thenReturn(null);

        when(factory.getPropertyResolver(any(PropertyResolver.Type.class))).thenReturn(propertyResolver);
        when(entityCrud.save(serviceToMigrate)).thenReturn(1234L);

        try {
            manager.importBundle(bundle, false);
            fail("Expected MigrationException");
        } catch (final MigrationApi.MigrationException e) {
            assertEquals("Encapsulated Assertion not found for header: encapsulatedAssertion. " +
                    "Please import the Encapsulated Assertion on the target prior to migrating.\n", e.getErrors());
            throw e;
        }
    }

    private MigrationBundle createServiceMigrationBundleWithEncassDependency() {
        final MigrationMetadata metadata = new MigrationMetadata();
        final MigrationBundle bundle = new MigrationBundle(metadata);
        // we are migrating a service
        final PublishedService serviceToMigrate = new PublishedService();
        final ExternalEntityHeader serviceHeader = new ExternalEntityHeader(new Goid(0,1).toHexString(), EntityType.SERVICE, new Goid(0,1).toHexString(), "service", "usesEncapsulatedAssertion", 0);
        metadata.addHeader(serviceHeader);
        bundle.addExportedItem(new ExportedItem(serviceHeader, serviceToMigrate));
        metadata.setTargetFolder(new ExternalEntityHeader(new Goid(0,1).toHexString(), EntityType.FOLDER, new Goid(0,1).toHexString(), "RootFolder", "RootFolder", 0));

        // service depends on encass (expects the encass to exist on the target ssg)
        final ExternalEntityHeader encassHeader = new ExternalEntityHeader(new Goid(0,1).toHexString(), EntityType.ENCAPSULATED_ASSERTION, new Goid(0,1).toHexString(), "encapsulatedAssertion", "encapsulatedAssertion", 0);
        metadata.addDependency(new MigrationDependency(serviceHeader, encassHeader, "Policy:"+new Goid(0,2).toHexString()+":EntitiesUsed", PropertyResolver.Type.ASSERTION, MigrationMappingSelection.NONE, false));
        return bundle;
    }

    private EntityHeaderWithType hasEntityType(final EntityType entityType) {
        return new EntityHeaderWithType(entityType);
    }

    class EntityHeaderWithType extends ArgumentMatcher<EntityHeader> {
        EntityType entityType;

        EntityHeaderWithType(final EntityType entityType) {
            this.entityType = entityType;
        }

        @Override
        public boolean matches(final Object o) {
            if (o != null) {
                final EntityHeader header = (EntityHeader) o;
                return entityType == header.getType();
            }
            return false;
        }
    }
}
