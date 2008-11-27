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

    public EntityHeaderSet<EntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationException;

    // is this one optional? maybe remove from interface
    public MigrationMetadata findDependencies(Collection<EntityHeader> headers) throws MigrationException;

    public MigrationBundle exportBundle(Set<EntityHeader> headers) throws MigrationException;

    public Map<EntityHeader, EntityHeaderSet> retrieveMappingCandidates(Set<EntityHeader> mappables) throws MigrationException;

    public void importBundle(MigrationBundle bundle) throws MigrationException;
}
