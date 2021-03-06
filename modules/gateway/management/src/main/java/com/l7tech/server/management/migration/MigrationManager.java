package com.l7tech.server.management.migration;

import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.ExternalEntityHeader;

import java.util.Map;
import java.util.Collection;

public interface MigrationManager {

    public Collection<ExternalEntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationApi.MigrationException;

    public Collection<ExternalEntityHeader> checkHeaders(Collection<ExternalEntityHeader> headers);

    public MigrationMetadata findDependencies(Collection<ExternalEntityHeader> headers) throws MigrationApi.MigrationException;

    public MigrationBundle exportBundle(Collection<ExternalEntityHeader> headers) throws MigrationApi.MigrationException;

    public Map<ExternalEntityHeader, EntityHeaderSet<ExternalEntityHeader>> retrieveMappingCandidates(Collection<ExternalEntityHeader> mappables, ExternalEntityHeader scope, Map<String,String> filters) throws MigrationApi.MigrationException;

    public Collection<MigratedItem> importBundle(MigrationBundle bundle, boolean dryRun) throws MigrationApi.MigrationException;
}
