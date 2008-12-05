package com.l7tech.server.management.migration;

import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.migration.MigrationException;

import java.util.Set;
import java.util.Map;
import java.util.Collection;

public interface MigrationManager {

    public Collection<EntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationException;

    public MigrationMetadata findDependencies(Collection<EntityHeader> headers) throws MigrationException;

    public MigrationBundle exportBundle(Collection<EntityHeader> headers) throws MigrationException;

    public Map<EntityHeader, EntityHeaderSet> retrieveMappingCandidates(Collection<EntityHeader> mappables, String filter) throws MigrationException;

    public void importBundle(MigrationBundle bundle, EntityHeader targetFolder, boolean flattenFolders, boolean overwriteExisting) throws MigrationException;
}
