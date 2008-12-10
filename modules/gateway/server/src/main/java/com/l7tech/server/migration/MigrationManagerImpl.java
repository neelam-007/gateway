package com.l7tech.server.migration;

import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.ExportedItem;
import com.l7tech.server.management.migration.bundle.MigratedItem;
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
import com.l7tech.gateway.common.service.PublishedService;

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
        Collection<EntityHeader> resolvedHeaders = resolveHeaders( headers );

        MigrationMetadata result = new MigrationMetadata();
        result.setHeaders( resolvedHeaders );

        for ( EntityHeader header : resolvedHeaders ) {
            findDependenciesRecursive(result, header);
        }

        return result;
    }

    @Override
    public MigrationBundle exportBundle(final Collection<EntityHeader> headers) throws MigrationException {
        MigrationMetadata metadata = findDependencies(headers);
        MigrationBundle bundle = new MigrationBundle(metadata);
        for (EntityHeader header : metadata.getHeaders()) {
            if (!metadata.includeInExport(header)) {
                logger.log(Level.FINEST, "Not exporting header {0}", header);
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
    public Collection<MigratedItem> importBundle(MigrationBundle bundle, EntityHeader targetFolder,
                                                 boolean flattenFolders, boolean overwriteExisting, boolean enableServices, boolean dryRun) throws MigrationException {
        logger.log(Level.FINEST, "Importing bundle: {0}", bundle);

        processFolders(bundle, targetFolder, flattenFolders);

        MigrationErrors errors = validateBundle(bundle);
        if (!errors.isEmpty())
            logger.log(Level.WARNING, "Migration bundle validation failed.", errors);
        // todo: enable strict validation : throw new MigrationException("Migration bundle validation failed.", errors);

        Map<EntityHeader, EntityOperation> entities = loadEntities(bundle, overwriteExisting, enableServices);

        applyMappings(bundle, entities);
        if (!errors.isEmpty())
            logger.log(Level.WARNING, "Errors while applying mappings for the entities to import.", errors);
        // todo: throw new MigrationException("Errors while applying mappings for the entities to import.", errors);

        try {
            Collection<MigratedItem> result = new HashSet<MigratedItem>();
            Set<EntityHeader> uploaded = new HashSet<EntityHeader>();
            // add to result
            for(EntityHeader header : bundle.getMetadata().getHeaders()) {
                if (! uploaded.contains(header)) {
                    uploadEntityRecursive(header, entities, bundle.getMetadata(), uploaded, dryRun);
                }
                EntityOperation eo = entities.get(header);
                if (eo.operation != IGNORE) {
                    result.add(new MigratedItem(header, EntityHeaderUtils.fromEntity(eo.entity), dryRun ? eo.operation.toString() : eo.operation.toString() + "D"));
                }
            }
            return result;
        } catch (ObjectModelException e) {
            throw new MigrationException("Import failed.", e);
        }
    }

    private void uploadEntityRecursive(EntityHeader header, Map<EntityHeader, EntityOperation> entities, MigrationMetadata metadata, Set<EntityHeader> uploaded, boolean dryRun) throws MigrationException, UpdateException, SaveException {

        if (uploaded.contains(header)) {
            logger.log(Level.WARNING, "Circular dependency reached during entity upload for header {0}.", header);
            return;
        }
        uploaded.add(header);

        // upload dependencies first
        Set<MigrationMapping> dependencies = metadata.getMappingsForSource(header);
        if (dependencies != null) {
            for (MigrationMapping mapping : dependencies) {
                EntityHeader dep = metadata.getHeader(mapping.getDependency());
                if (dep != null)
                    uploadEntityRecursive(dep, entities, metadata, uploaded, dryRun);
                else
                    logger.log(Level.WARNING, "Header not found for dependency reference {0}", mapping.getDependency());
            }
        }

        if (! dryRun) {
            EntityOperation eo = entities.get(header);
            switch (eo.operation)  {
                case IGNORE:
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
    }


    private void processFolders(MigrationBundle bundle, EntityHeader targetFolder, boolean flatten) throws MigrationException {
        MigrationMetadata metadata = bundle.getMetadata();
        if (flatten) {
            // replace all folder dependencies withe the (unique) targetFolder
            Set<EntityHeaderRef> headersToRemove = new HashSet<EntityHeaderRef>();
            for (MigrationMapping mapping : metadata.getMappings()) {
                if (mapping.getDependency().getType() == EntityType.FOLDER) {
                    headersToRemove.add(mapping.getDependency());
                    mapping.mapDependency(targetFolder, false);
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
            metadata.mapName(ROOT_FOLDER_REF, targetFolder, false);
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private MigrationErrors applyMappings(MigrationBundle bundle, Map<EntityHeader, EntityOperation> entities) throws MigrationException {
        MigrationErrors errors = new MigrationErrors();

        for (EntityHeader header : entities.keySet()) {
            EntityOperation eo = entities.get(header);

            MigrationMetadata metadata = bundle.getMetadata();
//            if ( eo.operation == IGNORE && ! metadata.isUploadedByParent(header))
//                continue;

            try {
                for (MigrationMapping mapping : metadata.getMappingsForSource(header)) {
                    PropertyResolver resolver = getResolver(eo.entity, mapping.getPropName());
                    EntityOperation targetEo = entities.get(metadata.getHeader(mapping.getDependency()));
                    if (targetEo == null || targetEo.entity == null) {
                        errors.add(EntityHeaderUtils.fromEntity(eo.entity), new MigrationException("Cannot apply mapping, target entity not found for dependency reference: " + mapping.getDependency()));
                        continue;
                    }
                    resolver.applyMapping(eo.entity, mapping.getPropName(), targetEo.entity, metadata.getOriginalHeader(mapping.getSourceDependency()));
                }
            } catch (MigrationException e) {
                logger.log(Level.WARNING, "Errors while applying mapping.", e);
                errors.add(EntityHeaderUtils.fromEntity(eo.entity), e);
            }
        }
        return errors;
    }

    static enum ImportOperation { CREATE, UPDATE, IGNORE }

    private static class EntityOperation {
        Entity entity;
        ImportOperation operation;
        private EntityOperation(Entity entity, ImportOperation operation) {
            this(entity, operation, false);
        }
        private EntityOperation(Entity entity, ImportOperation operation, boolean createServiceDisabled) {
            this.entity = entity;
            this.operation = operation;
            if (entity instanceof PublishedService && operation == CREATE && createServiceDisabled)
                ((PublishedService)entity).setDisabled(true);
        }
    }

    private Map<EntityHeader,EntityOperation> loadEntities(MigrationBundle bundle, boolean overwriteExisting, boolean enableServices) throws MigrationException {

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
                result.put(header, new EntityOperation(fromBundle, IGNORE));
            } else if (existing == null) {
                if (fromBundle instanceof PersistentEntity)
                    ((PersistentEntity)fromBundle).setOid(PersistentEntity.DEFAULT_OID);
                result.put(header, new EntityOperation(fromBundle, CREATE, !enableServices));
            } else if (fromBundle == null) {
                result.put(header, new EntityOperation(existing, IGNORE));
            } else if (overwriteExisting) { // both not null
                if (fromBundle instanceof PersistentEntity && existing instanceof PersistentEntity)
                    ((PersistentEntity)fromBundle).setOid(((PersistentEntity)existing).getOid());
                result.put(header, new EntityOperation(fromBundle, UPDATE));
            } else {
                result.put(header, new EntityOperation(existing, IGNORE));
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
            EntityHeaderRef header = mapping.getDependant();
            if (! metadata.hasHeader(header))
                errors.add(header, new MigrationException("Header listed as the source of a dependency, but not included in bundle metadata: " + header));
            header = mapping.getDependency();
            if (! metadata.hasHeader(header))
                errors.add(header, new MigrationException("Header listed as a dependency, but not included in bundle metadata: " + header));

            // name-mapping required
            if (mapping.getType().getNameMapping() == REQUIRED && ! mapping.isMappedDependency())
                errors.add(mapping, new MigrationException("Unresolved name-mapping: " + mapping));

            // value-mapping required
            if (! mapping.isMappedDependency() && mapping.getType().getValueMapping() == REQUIRED &&
                (bundle.getExportedItem(mapping.getDependency()) == null || ! bundle.getExportedItem(mapping.getDependency()).isMappedValue()) ) {
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
            logger.log(Level.WARNING, "Error loading entity for dependency '"+header+"'.", ExceptionUtils.getDebugException(e));
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
                    for ( MigrationMapping mapping : deps.get(depHeader) ) {
                        EntityHeader resolvedHeader = resolveHeader( depHeader );
                        if ( depHeader instanceof GuidEntityHeader ) {
                            // TODO find a better way to do this
                            EntityHeader resolvedDepHeader  = resolveHeader( depHeader );
                            if ( mapping.getDependant().getStrId().equals(((GuidEntityHeader)depHeader).getGuid()) ) {
                                mapping.setDependant( EntityHeaderRef.fromOther(resolvedDepHeader) );
                            }
                            if ( mapping.getDependency().getStrId().equals(((GuidEntityHeader)depHeader).getGuid()) ) {
                                mapping.setSourceDependency( EntityHeaderRef.fromOther(resolvedDepHeader) );
                            }
                        }
                        result.addMapping(mapping);
                        logger.log(Level.FINE, "Added mapping: " + mapping);
                        if ( !result.hasHeader( resolvedHeader ) ) {
                            findDependenciesRecursive( result, resolvedHeader );
                        }
                    }
                }
            }
        }
    }

    private EntityHeader resolveHeader( final EntityHeader header ) throws MigrationException {
        return EntityHeaderUtils.fromEntity( loadEntity(header) );
    }

    private Collection<EntityHeader> resolveHeaders( final Collection<EntityHeader> headers ) throws MigrationException {
        List<EntityHeader> resolvedHeaders = new ArrayList<EntityHeader>( headers.size() );

        for ( EntityHeader header : headers ) {
            resolvedHeaders.add( resolveHeader( header ) );
        }

        return resolvedHeaders;
    }

    private Entity loadEntity( final EntityHeader header ) throws MigrationException {
        logger.log(Level.FINEST, "Loading entity for header: {0}", header);
        try {
            Entity ent = entityCrud.find(header); // load the entity
            if (ent == null)
                throw new MigrationException("Error loading the entity for header "+ header.getType() +", " + header.getName() + "(#"+header.getOid()+")");
            return ent;
        } catch (FindException e) {
            throw new MigrationException("Error loading the entity for header: " + header, e);
        }
    }
}
