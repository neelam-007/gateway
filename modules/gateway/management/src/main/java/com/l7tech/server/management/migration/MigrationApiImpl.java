package com.l7tech.server.management.migration;

import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.migration.MigrationException;

import java.util.Collection;
import java.util.Set;
import java.util.Map;

/**
 * 
 * @author jbufu
 */
public class MigrationApiImpl implements MigrationApi {

    private MigrationManager manager;

    public MigrationApiImpl(MigrationManager manager) {
        this.manager = manager;
    }

    public EntityHeaderSet<EntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationException {
        return manager.listEntities(clazz);
    }

    public MigrationMetadata findDependencies(Collection<EntityHeader> headers) throws MigrationException {
        return manager.findDependencies(headers);
    }

    public MigrationBundle exportBundle(Set<EntityHeader> headers) throws MigrationException {
        return manager.exportBundle(headers);
    }

    public Map<EntityHeader, EntityHeaderSet> retrieveMappingCandidates(Set<EntityHeader> mappables) throws MigrationException {
        return manager.retrieveMappingCandidates(mappables);
    }

    public void importBundle(MigrationBundle bundle) throws MigrationException {
        manager.importBundle(bundle);
    }
}
