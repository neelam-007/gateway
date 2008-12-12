package com.l7tech.server.management.migration;

import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;

import java.util.Map;
import java.util.Collection;

public interface MigrationManager {

    public Collection<EntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationApi.MigrationException;

    public MigrationMetadata findDependencies(Collection<EntityHeader> headers) throws MigrationApi.MigrationException;

    public MigrationBundle exportBundle(Collection<EntityHeader> headers) throws MigrationApi.MigrationException;

    public Map<EntityHeader, EntityHeaderSet> retrieveMappingCandidates(Collection<EntityHeader> mappables, String filter) throws MigrationApi.MigrationException;

    public Collection<MigratedItem> importBundle(MigrationBundle bundle, EntityHeader targetFolder,
                                                 boolean flattenFolders, boolean overwriteExisting, boolean enableServices, boolean dryRun) throws MigrationApi.MigrationException;
}
