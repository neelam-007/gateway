package com.l7tech.server.migration;

import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.server.management.migration.MigrationManager;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.util.ExceptionUtils;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author jbufu
 */
public class MigrationApiImpl implements MigrationApi {

    private static final Logger logger = Logger.getLogger(MigrationApiImpl.class.getName());

    private MigrationManager manager;

    public MigrationApiImpl(MigrationManager manager) {
        this.manager = manager;
    }

    @Override
    public Collection<ExternalEntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationException {
        return manager.listEntities(clazz);
    }

    @Override
    public Collection<ExternalEntityHeader> checkHeaders(Collection<ExternalEntityHeader> headers) {
        return manager.checkHeaders(headers);
    }

    @Override
    public MigrationMetadata findDependencies(Collection<ExternalEntityHeader> headers) throws MigrationException {
        return manager.findDependencies(headers);
    }

    @Override
    public MigrationBundle exportBundle(Collection<ExternalEntityHeader> headers) throws MigrationException {
        try {
            return manager.exportBundle(headers);
        } catch ( MigrationException e ) {
            logger.log( Level.WARNING, "Error in Migration API '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );
            throw e;
        }
    }

    @Override
    public Map<ExternalEntityHeader, EntityHeaderSet<ExternalEntityHeader>> retrieveMappingCandidates(Collection<ExternalEntityHeader> mappables, ExternalEntityHeader scope, final Map<String,String> filters) throws MigrationException {
        return manager.retrieveMappingCandidates(mappables, scope, filters);
    }

    @Override
    public Collection<MigratedItem> importBundle(MigrationBundle bundle, boolean dryRun) throws MigrationException {
        boolean oldSystem = AuditContextUtils.isSystem();
        try {
            AuditContextUtils.setSystem(true);
            return manager.importBundle(bundle, dryRun);
        } catch ( MigrationException e ) {
            logger.log( Level.WARNING, "Error in Migration API '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );
            throw e;
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Unexpected error in Migration API.", e );
            throw ExceptionUtils.wrap(e);
        } finally {
            AuditContextUtils.setSystem(oldSystem);
        }
    }
}
