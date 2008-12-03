package com.l7tech.server.migration;

import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.ExportedItem;
import com.l7tech.server.management.migration.MigrationManager;
import com.l7tech.objectmodel.migration.MigrationException;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.*;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.*;
import static com.l7tech.objectmodel.migration.MigrationException.*;
import com.l7tech.server.*;
import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;

import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements MigrationManager operations for SSG.
 *
 * @see com.l7tech.server.management.migration.MigrationManager
 * @author jbufu
 */
public class MigrationManagerImpl implements MigrationManager {

    private static final Logger logger = Logger.getLogger(MigrationManagerImpl.class.getName());

    private PlatformTransactionManager transactionManager;
    private EntityFinder entityFinder;
    private EntityCrud entityCrud;
    private PropertyResolverFactory resolverFactory;

    public MigrationManagerImpl(PlatformTransactionManager transactionManager, EntityFinder entityFinder, EntityCrud entityCrud, PropertyResolverFactory resolverFactory) {
        this.entityFinder = entityFinder;
        this.transactionManager = transactionManager;
        this.entityCrud = entityCrud;
        this.resolverFactory = resolverFactory;
    }

    public Collection<EntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationException {
        logger.log(Level.FINEST, "Listing entities for class: {0}", clazz.getName());
        try {
            return entityFinder.findAll(clazz);
        } catch (FindException e) {
            throw new MigrationException("Error listing entities for " + clazz + " : " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    public MigrationMetadata findDependencies(Collection<EntityHeader> headers) throws MigrationException {
        logger.log(Level.FINEST, "Finding dependencies for headers: {0}", headers);
        MigrationMetadata result = new MigrationMetadata();
        result.setHeaders(headers);

        for (EntityHeader header : headers) {
            findDependenciesRecursive(result, header);
        }

        return result;
    }

    public MigrationBundle exportBundle(final Collection<EntityHeader> headers) throws MigrationException {

        final MigrationException[] thrown = new MigrationException[1];
        MigrationBundle result = (MigrationBundle) new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                try {
                    MigrationMetadata metadata = findDependencies(headers);
                    MigrationBundle bundle = new MigrationBundle(metadata);
                    for (EntityHeader header : metadata.getHeaders()) {
                        if (metadata.isMappingRequired(header)) {
                            logger.log(Level.FINEST, "Mapping required for {0}, not exporting.", header);
                            continue;
                        }

                        Entity ent = loadEntity(header);
                        logger.log(Level.FINE, "Entity value for header {0} : {1}", new Object[] {header.toStringVerbose(), ent});
                        bundle.addExportedItem(new ExportedItem(header, ent));
                    }
                    return bundle;
                } catch (MigrationException e) {
                    thrown[0] = e;
                    return null;
                }
            }
        });

        if (thrown[0] != null)
            throw thrown[0];
        else
            return result;
    }

    public Map<EntityHeader, EntityHeaderSet> retrieveMappingCandidates(Collection<EntityHeader> mappables) throws MigrationException {
        logger.log(Level.FINEST, "Retrieving mapping candidates for {0}.", mappables);
        Map<EntityHeader,EntityHeaderSet> result = new HashMap<EntityHeader,EntityHeaderSet>();

        for (EntityHeader header : mappables) {
            try {
                EntityHeaderSet<EntityHeader> candidates = entityFinder.findAll(EntityHeaderUtils.getEntityClass(header));
                logger.log(Level.FINEST, "Found {0} mapping candidates for header {1}.", new Object[]{candidates != null ? candidates.size() : 0,header});
                result.put(header, candidates);
            } catch (FindException e) {
                throw new MigrationException("Error retrieving mapping candidate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        return result;
    }


    @Transactional(rollbackFor = Throwable.class)
    public void importBundle(MigrationBundle bundle) throws MigrationException {
        logger.log(Level.FINEST, "Importing bundle: {0}", bundle);
        MigrationErrors errors = validateBundle(bundle);
        if (! errors.isEmpty())
            logger.log(Level.WARNING, "Migration bundle validation failed.", errors);
            // todo: enable strict validation : throw new MigrationException("Migration bundle validation failed.", errors);

        // load entities
        Map<EntityHeaderRef, Entity> entities = new HashMap<EntityHeaderRef, Entity>();
        Map<EntityHeader, Entity> entitiesToImport = new HashMap<EntityHeader, Entity>();
        for (EntityHeader header : bundle.getMetadata().getHeaders()) {
            ExportedItem exportedItem;
            Entity ent;
            try {
                // try the local ssg first
                entities.put(header, loadEntity(header));
            } catch (MigrationException e) {
                // load it from the bundle
                exportedItem = bundle.getExportedItem(header);
                ent = exportedItem == null ? null : exportedItem.getValue();
                if (ent == null) { // should have been caught by bundle validation
                    logger.log(Level.WARNING, "Entity not found for header {0}. (unresolved mapping not validated?).", header);
                    continue;
                    // todo: throw new MigrationException("Entity not found for header {0}. (unresolved mapping not validated?).");
                }
                entities.put(header, ent);
                if (! bundle.getMetadata().isUploadedByParent(header))
                    entitiesToImport.put(header, ent);
            }
        }

        // apply mappings
        Entity sourceEntity, targetEntity;
        for (EntityHeader header : entitiesToImport.keySet()) {
            sourceEntity = entitiesToImport.get(header);
            try {
                for (MigrationMapping mapping : bundle.getMetadata().getMappingsForSource(header)) {
                    targetEntity = entities.get(mapping.getTarget());
                    PropertyResolver resolver = getResolver(sourceEntity, mapping.getPropName());
                    try {
                        resolver.applyMapping(sourceEntity, mapping.getPropName(), targetEntity);
                    } catch (MigrationException e) {
                        logger.log(Level.WARNING, "Errors while applying mapping: ", e);
                        errors.add(mapping, e);
                    }
                }
            } catch (MigrationException e) {
                logger.log(Level.WARNING, "Errors while applying mapping: ", e);
                errors.add(header, e);
            }
        }
        if (! errors.isEmpty())
            logger.log(Level.WARNING, "Errors while applying mappings for the entities to import.", errors);
            // todo: throw new MigrationException("Errors while applying mappings for the entities to import.", errors);

        // upload
        for (Entity entity : entitiesToImport.values()) {
            try {
                entityCrud.save(entity);
            } catch (SaveException e) {
                throw new MigrationException("Import failed.", e);
            }
        }
    }

    private PropertyResolver getResolver(Entity sourceEntity, String propName) throws MigrationException {

        PropertyResolver annotatedResolver = MigrationUtils.getResolver(sourceEntity, propName);

        if (annotatedResolver != null && ! annotatedResolver.getClass().equals(DefaultEntityPropertyResolver.class))
            return annotatedResolver;
        else
            return resolverFactory.getPropertyResolver(MigrationUtils.getTargetType(sourceEntity, propName));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private MigrationErrors validateBundle(MigrationBundle bundle) {
        logger.log(Level.FINEST, "Validating bundle: {0}", bundle);
        MigrationErrors errors = new MigrationErrors();
        MigrationMetadata metadata = bundle.getMetadata();

        // check that entity values are available for all headers, either in the bundle or already on the SSG
        for (EntityHeader header : metadata.getHeaders()) {
            if (! bundle.hasItem(header)) {
                try {
                    if (loadEntity(header) == null)
                        errors.add(header, new MigrationException("Null entity retrived for header: " + header));
                } catch (MigrationException e) {
                    errors.add(header, e);
                }
            }
        }

        // dependency-check covered by the above and mapping targets check below

        // mapping requirements                                                                                                                                s
        for(MigrationMapping mapping : metadata.getMappings()) {

            // all headers present in the metadata
            EntityHeaderRef header = mapping.getSource();
            if (! metadata.hasHeader(header))
                errors.add(header, new MigrationException("Header listed as the source of a dependency, but not included in bundle metadata: " + header));
            header = mapping.getTarget();
            if (! metadata.hasHeader(header))
                errors.add(header, new MigrationException("Header listed as a dependency, but not included in bundle metadata: " + header));

            // name-mapping required
            if (mapping.getType().getNameMapping() == REQUIRED && ! mapping.isMappedTarget())
                errors.add(mapping, new MigrationException("Unresolved name-mapping: " + mapping));

            // value-mapping required
            if (! mapping.isMappedTarget() && mapping.getType().getValueMapping() == REQUIRED &&
                ! bundle.getExportedItem(mapping.getTarget()).isMappedValue()) {
                errors.add(mapping, new MigrationException("Unresolved value-mapping: " + mapping));
            }
        }

        return errors;
    }

    private void findDependenciesRecursive(MigrationMetadata result, EntityHeader header) throws MigrationException {
        logger.log(Level.FINE, "Finding dependencies for: " + header.toStringVerbose());
        result.addHeader(header); // marks header as processed
        Entity entity;
        try {
            entity = loadEntity(header);
        } catch (Exception e) {
            return;
        }
        for (Method method : entity.getClass().getMethods()) {
            if (MigrationUtils.isDependency(method)) {
                PropertyResolver resolver = getResolver(entity, method.getName());
                Map<EntityHeader, Set<MigrationMapping>> deps;
                try {
                    deps = resolver.getDependencies(header, entity, method);
                } catch (MigrationException e) {
                    throw new MigrationException("Error getting dependencies for property: " + method, e);
                }
                for (EntityHeader depHeader : deps.keySet()) {
                    for (MigrationMapping mapping : deps.get(depHeader)) {
                        result.addMapping(mapping);
                        logger.log(Level.FINE, "Added mapping: " + mapping);
                        if (!result.hasHeader(depHeader))
                            findDependenciesRecursive(result, depHeader);
                    }
                }
            }
        }
    }

    private Entity loadEntity(EntityHeader header) throws MigrationException {
        logger.log(Level.FINEST, "Loading entity for header: {0}", header);
        try {
            Entity ent = entityFinder.find(header); // load the entity
            if (ent == null)
                throw new MigrationException("Error loading the entity for header: " + header);
            return ent;
        } catch (FindException e) {
            throw new MigrationException("Error loading the entity for header: " + header, e);
        }
    }
}
