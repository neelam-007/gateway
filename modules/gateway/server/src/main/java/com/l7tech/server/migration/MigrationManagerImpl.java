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
import static com.l7tech.server.migration.MigrationManagerImpl.ImportOperation.*;
import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;

import org.springframework.transaction.annotation.Transactional;

/**
 * Implements MigrationManager operations for SSG.
 *
 * @see com.l7tech.server.management.migration.MigrationManager
 * @author jbufu
 */
@Transactional(rollbackFor = Throwable.class)
public class MigrationManagerImpl implements MigrationManager {

    private static final Logger logger = Logger.getLogger(MigrationManagerImpl.class.getName());

    private EntityCrud entityCrud;
    private PropertyResolverFactory resolverFactory;
    private static final EntityHeaderRef ROOT_FOLDER_REF = new EntityHeaderRef(EntityType.FOLDER, "-5002");

    public MigrationManagerImpl(EntityCrud entityCrud, PropertyResolverFactory resolverFactory) {
        this.entityCrud = entityCrud;
        this.resolverFactory = resolverFactory;
    }

    @Override
    public Collection<EntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationException {
        logger.log(Level.FINEST, "Listing entities for class: {0}", clazz.getName());
        try {
            return entityCrud.findAll(clazz);
        } catch (FindException e) {
            throw new MigrationException("Error listing entities for " + clazz + " : " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    @Override
    public MigrationMetadata findDependencies(Collection<EntityHeader> headers ) throws MigrationException {
        logger.log(Level.FINEST, "Finding dependencies for headers: {0}", headers);
        MigrationMetadata result = new MigrationMetadata();
        result.setHeaders(headers);

        for (EntityHeader header : headers) {
            findDependenciesRecursive(result, header);
        }

        return result;
    }

    @Override
    public MigrationBundle exportBundle(final Collection<EntityHeader> headers) throws MigrationException {
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
    }

    @Override
    public Map<EntityHeader, EntityHeaderSet> retrieveMappingCandidates(Collection<EntityHeader> mappables, String filter) throws MigrationException {
        logger.log(Level.FINEST, "Retrieving mapping candidates for {0}.", mappables);
        Map<EntityHeader,EntityHeaderSet> result = new HashMap<EntityHeader,EntityHeaderSet>();

        for (EntityHeader header : mappables) {
            try {
                EntityHeaderSet<EntityHeader> candidates = entityCrud.findAll(EntityHeaderUtils.getEntityClass(header), filter, 0, 50 );
                logger.log(Level.FINEST, "Found {0} mapping candidates for header {1}.", new Object[]{candidates != null ? candidates.size() : 0, header});
                result.put(header, candidates);
            } catch (FindException e) {
                throw new MigrationException("Error retrieving mapping candidate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        return result;
    }


    @Override
    public void importBundle(MigrationBundle bundle, EntityHeader targetFolder, boolean flattenFolders, boolean overwriteExisting) throws MigrationException {
        logger.log(Level.FINEST, "Importing bundle: {0}", bundle);

        processFolders(bundle, targetFolder, flattenFolders);

        MigrationErrors errors = validateBundle(bundle);
        if (!errors.isEmpty())
            logger.log(Level.WARNING, "Migration bundle validation failed.", errors);
        // todo: enable strict validation : throw new MigrationException("Migration bundle validation failed.", errors);

        Map<EntityHeader, EntityOperation> entities = loadEntities(bundle, overwriteExisting);

        applyMappings(bundle, entities);
        if (!errors.isEmpty())
            logger.log(Level.WARNING, "Errors while applying mappings for the entities to import.", errors);
        // todo: throw new MigrationException("Errors while applying mappings for the entities to import.", errors);

        try {
            Set<EntityHeader> uploaded = new HashSet<EntityHeader>();
            for(EntityHeader header : bundle.getMetadata().getHeaders()) {
                uploadEntityRecursive(header, entities, bundle.getMetadata(), uploaded);
            }
        } catch (ObjectModelException e) {
            throw new MigrationException("Import failed.", e);
        }
    }

    private void uploadEntityRecursive(EntityHeader header, Map<EntityHeader, EntityOperation> entities, MigrationMetadata metadata, Set<EntityHeader> uploaded) throws MigrationException, UpdateException, SaveException {

        if (header == null || uploaded.contains(header))
            return; // circular dependency
        uploaded.add(header);

        // upload dependencies first
        Set<MigrationMapping> dependencies = metadata.getMappingsForSource(header);
        if (dependencies != null) {
            for (MigrationMapping mapping : dependencies) {
                uploadEntityRecursive(metadata.getHeader(mapping.getTarget()), entities, metadata, uploaded);
            }
        }

        EntityOperation eo = entities.get(header);
        switch (eo.operation)  {
            case SKIP:
                return;

            case UPDATE:
                entityCrud.update(eo.entity);
                break;

            case CREATE:
                Long oid = (Long)entityCrud.save(eo.entity);
                if (eo.entity instanceof PersistentEntity)
                    ((PersistentEntity)eo.entity).setOid(oid);
                break;

            default:
                throw new IllegalStateException("Import operation not known: " + eo.operation);
        }
    }


    private void processFolders(MigrationBundle bundle, EntityHeader targetFolder, boolean flatten) throws MigrationException {
        MigrationMetadata metadata = bundle.getMetadata();
        if (flatten) {
            // replace all folder dependencies withe the (unique) targetFolder
            Set<EntityHeaderRef> headersToRemove = new HashSet<EntityHeaderRef>();
            for (MigrationMapping mapping : metadata.getMappings()) {
                if (mapping.getTarget().getType() == EntityType.FOLDER) {
                    headersToRemove.add(mapping.getTarget());
                    mapping.setTarget(targetFolder);
                }
            }
            for (EntityHeaderRef headerRef : headersToRemove) {
                metadata.removeHeader(headerRef);
            }
            metadata.addHeader(targetFolder);
        } else {
            // map root folder to target folder
            if (!metadata.hasHeader(ROOT_FOLDER_REF))
                throw new MigrationException("Root folder not found in the bundle.");
            metadata.mapName(ROOT_FOLDER_REF, targetFolder);
        }
    }

    private MigrationErrors applyMappings(MigrationBundle bundle, Map<EntityHeader, EntityOperation> entities) throws MigrationException {
        MigrationErrors errors = new MigrationErrors();

        for (EntityHeader header : entities.keySet()) {
            EntityOperation eo = entities.get(header);

            MigrationMetadata metadata = bundle.getMetadata();
            if ( eo.operation == SKIP && ! metadata.isUploadedByParent(header))
                continue;

            try {
                for (MigrationMapping mapping : metadata.getMappingsForSource(header)) {
                    PropertyResolver resolver = getResolver(eo.entity, mapping.getPropName());
                    EntityOperation targetEo = entities.get(metadata.getHeader(mapping.getTarget()));
                    if (targetEo == null || targetEo.entity == null) {
                        // todo throw
                        logger.log(Level.WARNING, "Cannot apply mapping, target entity not found: {0} : {1}", new Object[]{eo.entity, mapping.getPropName()});
                        continue;
                    }
                    resolver.applyMapping(eo.entity, mapping.getPropName(), targetEo.entity, metadata.getOriginalHeader(mapping.getOriginalTarget()));
                }
            } catch (MigrationException e) {
                logger.log(Level.WARNING, "Errors while applying.", e);
                errors.add(EntityHeaderUtils.fromEntity(eo.entity), e);
            }
        }
        return errors;
    }

    static enum ImportOperation { CREATE, UPDATE, SKIP }

    private static class EntityOperation {
        Entity entity;
        ImportOperation operation;
        private EntityOperation(Entity entity, ImportOperation operation) {
            this.entity = entity;
            this.operation = operation;
        }
    }

    private Map<EntityHeader,EntityOperation> loadEntities(MigrationBundle bundle, boolean overwriteExisting) throws MigrationException {

        Map<EntityHeader, EntityOperation> result = new HashMap<EntityHeader, EntityOperation>();

        Entity existing, fromBundle;
        ExportedItem item;
        MigrationMetadata metadata = bundle.getMetadata();
        for (EntityHeader header : metadata.getHeaders()) {
            try {
                existing = loadEntity(header);
            } catch (MigrationException e) {
                existing = null;
            }
            item = bundle.getExportedItem(header);
            fromBundle = item == null ? null : item.getValue();

            if (fromBundle == null && existing == null) {
                throw new MigrationException("Entity not found for header (unresolved mapping not validated?):" + header);
            }

            if (metadata.isUploadedByParent(header)) {
                result.put(header, new EntityOperation(fromBundle, SKIP));
            } else if (existing == null) {
                if (fromBundle instanceof PersistentEntity)
                    ((PersistentEntity)fromBundle).setOid(PersistentEntity.DEFAULT_OID);
                result.put(header, new EntityOperation(fromBundle, CREATE));
            } else if (fromBundle == null) {
                result.put(header, new EntityOperation(existing, SKIP));
            } else if (overwriteExisting) { // both not null
                if (fromBundle instanceof PersistentEntity && existing instanceof PersistentEntity)
                    ((PersistentEntity)fromBundle).setOid(((PersistentEntity)existing).getOid());
                result.put(header, new EntityOperation(fromBundle, UPDATE));
            } else {
                result.put(header, new EntityOperation(existing, SKIP));
            }
        }

        return result;
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

        Entity entity = null;
        try {
            entity = loadEntity(header);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error loading entity for dependency {0}", header);
        }
        if ( (header.getName() == null || header.getName().length() == 0) && entity != null )
            header = EntityHeaderUtils.fromEntity(entity);
        result.addHeader(header); // marks header as processed
        if (entity == null) return;

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
            Entity ent = entityCrud.find(header); // load the entity
            if (ent == null)
                throw new MigrationException("Error loading the entity for header: " + header);
            return ent;
        } catch (FindException e) {
            throw new MigrationException("Error loading the entity for header: " + header, e);
        }
    }
}
