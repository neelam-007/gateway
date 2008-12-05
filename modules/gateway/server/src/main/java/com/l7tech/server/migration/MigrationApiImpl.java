package com.l7tech.server.migration;

import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.MigrationManager;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.migration.MigrationException;
import com.l7tech.util.ExceptionUtils;

import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author jbufu
 */
public class MigrationApiImpl implements MigrationApi {

    private static final Logger logger = Logger.getLogger(MigrationApiImpl.class.getName());

    private MigrationManager manager;
    private AuditContext auditContext;

    public MigrationApiImpl(MigrationManager manager, AuditContext auditContext) {
        this.manager = manager;
        this.auditContext = auditContext;
    }

    @Override
    public Collection<EntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationException {
        try {
            return manager.listEntities(clazz);
        } catch ( RuntimeException re ) {
            logger.log( Level.WARNING, "Unexpected error in Migration API.", re );
            throw re;
        }
    }

    @Override
    public MigrationMetadata findDependencies(Collection<EntityHeader> headers) throws MigrationException {
        try {
            return manager.findDependencies(headers);
        } catch ( RuntimeException re ) {
            logger.log( Level.WARNING, "Unexpected error in Migration API.", re );
            throw re;
        }
    }

    @Override
    public MigrationBundle exportBundle(Collection<EntityHeader> headers) throws MigrationException {
        try {
            return manager.exportBundle(headers);
        } catch ( RuntimeException re ) {
            logger.log( Level.WARNING, "Unexpected error in Migration API.", re );
            throw re;
        }
    }

    @Override
    public Collection<MappingCandidate> retrieveMappingCandidates(Collection<EntityHeader> mappables, String filter) throws MigrationException {
        try {
            return MigrationApi.MappingCandidate.asCandidates(manager.retrieveMappingCandidates(mappables, filter));
        } catch ( RuntimeException re ) {
            logger.log( Level.WARNING, "Unexpected error in Migration API.", re );
            throw re;
        }
    }

    @Override
    public void importBundle(MigrationBundle bundle, EntityHeader targetFolder, boolean flattenFolders, boolean overwriteExisting) throws MigrationException {
        boolean oldSystem = auditContext.isSystem();
        try {
            auditContext.setSystem(true);
            manager.importBundle(bundle, targetFolder, flattenFolders, overwriteExisting);
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Unexpected error in Migration API.", e );
            if (e instanceof MigrationException) 
                throw (MigrationException)e;
            else
                throw ExceptionUtils.wrap(e);
        } finally {
            auditContext.setSystem(oldSystem);
        }
    }
}
