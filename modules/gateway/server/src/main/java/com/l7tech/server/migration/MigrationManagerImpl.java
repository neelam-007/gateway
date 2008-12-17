package com.l7tech.server.migration;

import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.ExportedItem;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.server.management.migration.MigrationManager;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.*;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.*;
import com.l7tech.server.*;
import com.l7tech.server.service.resolution.ResolutionManager;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import static com.l7tech.server.migration.MigrationManagerImpl.ImportOperation.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceDocument;

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
    private ResolutionManager resolutionManager;

    public MigrationManagerImpl(EntityCrud entityCrud, PropertyResolverFactory resolverFactory, ResolutionManager resolutionManager) {
        this.entityCrud = entityCrud;
        this.resolverFactory = resolverFactory;
        this.resolutionManager = resolutionManager;
    }

    @Override
    public Collection<EntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationApi.MigrationException {
        if ( clazz == null ) throw new MigrationApi.MigrationException("Missing required parameter.");
        logger.log(Level.FINEST, "Listing entities for class: {0}", clazz.getName());
        try {
            return entityCrud.findAll(clazz);
        } catch (FindException e) {
            throw new MigrationApi.MigrationException("Error listing entities for " + clazz + " : " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    @Override
    public Collection<EntityHeader> checkHeaders(Collection<EntityHeader> headers) {
        Collection<EntityHeader> result = new HashSet<EntityHeader>();

        if ( headers != null ) {
            for (EntityHeader header : headers) {
                try {
                    loadEntity(header);
                    result.add(header);
                } catch (MigrationApi.MigrationException e) {
                    // entity not found, header won't be returned
                }
            }
        }
        return result;
    }

    @Override
    public MigrationMetadata findDependencies(Collection<EntityHeader> headers ) throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Finding dependencies for headers: {0}", headers);
        MigrationMetadata result = new MigrationMetadata();

        if ( headers != null ) {
            Collection<EntityHeader> resolvedHeaders = resolveHeaders( headers );
            result.setHeaders( resolvedHeaders );

            for ( EntityHeader header : resolvedHeaders ) {
                try {
                    findDependenciesRecursive(result, header);
                } catch (PropertyResolverException e) {
                    throw new MigrationApi.MigrationException(composeErrorMessage("Error when processing entity", header, e));
                } catch (MigrationApi.MigrationException e) {
                    throw new MigrationApi.MigrationException(composeErrorMessage("Error when processing entity", header, e));
                }
            }
        }

        return result;
    }

    @Override
    public MigrationBundle exportBundle(final Collection<EntityHeader> headers) throws MigrationApi.MigrationException {
        if ( headers == null || headers.isEmpty() ) throw new MigrationApi.MigrationException("Missing required parameter.");        
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
    public Map<EntityHeader, EntityHeaderSet> retrieveMappingCandidates(Collection<EntityHeader> mappables, String filter) throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Retrieving mapping candidates for {0}.", mappables);
        Map<EntityHeader,EntityHeaderSet> result = new HashMap<EntityHeader,EntityHeaderSet>();

        if ( mappables != null ) {
            for (EntityHeader header : mappables) {
                try {
                    EntityHeaderSet<EntityHeader> candidates = entityCrud.findAll(EntityHeaderUtils.getEntityClass(header), filter, 0, 50 );
                    logger.log(Level.FINEST, "Found {0} mapping candidates for header {1}.", new Object[]{candidates != null ? candidates.size() : 0, header});
                    result.put(header, candidates);
                } catch (FindException e) {
                    throw new MigrationApi.MigrationException("Error retrieving mapping candidate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }

        return result;
    }


    @Override
    public Collection<MigratedItem> importBundle(MigrationBundle bundle, EntityHeader targetFolder,
                                                 boolean flattenFolders, boolean overwriteExisting, boolean enableServices, boolean dryRun) throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Importing bundle: {0}", bundle);
        if ( bundle == null ) throw new MigrationApi.MigrationException("Missing required parameter.");
        if ( targetFolder == null ) throw new MigrationApi.MigrationException("Missing required parameter.");                

        Collection<String> errors = processFolders(bundle, targetFolder, flattenFolders);

        errors.addAll(validateBundle(bundle));

        Map<EntityHeader, EntityOperation> entities = loadEntities(bundle, overwriteExisting, enableServices, errors);

        errors.addAll(checkServiceResolution(entities));

        if (!errors.isEmpty())
            logger.log(Level.WARNING, "Bundle validation errors: {0}.", errors);
            //throw new MigrationApi.MigrationException(errors);

        try {
            Collection<MigratedItem> result = new HashSet<MigratedItem>();
            Set<EntityHeader> uploaded = new HashSet<EntityHeader>();
            // add to result
            for(EntityHeader header : bundle.getMetadata().getHeaders()) {
                try {
                    if (! uploaded.contains(header)) {
                        uploadEntityRecursive(header, entities, bundle.getMetadata(), uploaded, dryRun);
                    }
                    EntityOperation eo = entities.get(header);
                    if (eo.operation != IGNORE) {
                        result.add(new MigratedItem(header, EntityHeaderUtils.fromEntity(eo.entity), dryRun ? eo.operation.toString() : eo.operation.toString() + "D"));
                    }
                } catch ( ObjectModelException ome ) {
                    throw new MigrationApi.MigrationException(composeErrorMessage("Import failed when processing entity", header, ome));
                }
            }
            return result;
        } catch (MigrationApi.MigrationException e) {
            throw e;
        } catch (Exception e) {
            throw new MigrationApi.MigrationException("Import failed.", e);
        }
    }

    private void uploadEntityRecursive(EntityHeader header, Map<EntityHeader, EntityOperation> entities, MigrationMetadata metadata, Set<EntityHeader> uploaded, boolean dryRun) throws UpdateException, SaveException, MigrationApi.MigrationException {

        if (uploaded.contains(header)) {
            logger.log(Level.WARNING, "Circular dependency reached during entity upload for header {0}.", header);
            return;
        }
        uploaded.add(header);

        EntityOperation eo = entities.get(header);
        if (eo == null) return;

        // upload dependencies first
        EntityHeaderRef headerForDependencies = eo.operation == UPDATE ? metadata.getUnMapped(header) : header;
        Set<MigrationMapping> dependencies = headerForDependencies != null ? metadata.getMappingsForSource(headerForDependencies) : null;
        if (dependencies != null) {
            for (MigrationMapping mapping : dependencies) {
                EntityHeader dep = metadata.getHeaderMappedOrOriginal(mapping.getDependency());
                if (dep != null)
                    uploadEntityRecursive(dep, entities, metadata, uploaded, dryRun);
                else
                    logger.log(Level.WARNING, "Header not found for dependency reference {0}", mapping.getDependency());
            }
        }

        if (! dryRun) {
            switch (eo.operation)  {
                case IGNORE:
                    break;

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

        // apply mappings for this entity's dependants
        try {
            applyMappings(header, metadata, entities);
        } catch (PropertyResolverException e) {
            throw new MigrationApi.MigrationException(e);
        }

    }

    private void applyMappings(EntityHeader dependency, MigrationMetadata metadata, Map<EntityHeader, EntityOperation> entities) throws MigrationApi.MigrationException, PropertyResolverException {
        for(MigrationMapping mapping : metadata.getMappingsForTarget(dependency)) {
            EntityHeader header = metadata.getHeaderMappedOrOriginal(mapping.getDependant());
            EntityOperation eo = entities.get(header);
            PropertyResolver resolver = getResolver(eo.entity, mapping.getPropName());
            EntityOperation targetEo = entities.get(dependency);
            if (targetEo == null || targetEo.entity == null) {
                throw new MigrationApi.MigrationException("Cannot apply mapping, target entity not found for dependency reference: " + mapping.getDependency());
            }
            resolver.applyMapping(eo.entity, mapping.getPropName(), targetEo.entity, metadata.getOriginalHeader(mapping.getSourceDependency()));
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Collection<String> processFolders(MigrationBundle bundle, EntityHeader targetFolder, boolean flatten) {
        Collection<String> errors = new HashSet<String>();
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
            if (!metadata.hasHeader(ROOT_FOLDER_REF)) {
                logger.log(Level.INFO, "Root folder not found in the bundle, nor processing folders.");
            } else {
                try {
                    metadata.mapName(ROOT_FOLDER_REF, targetFolder, false);
                } catch (MigrationApi.MigrationException e) {
                    errors.add(ExceptionUtils.getMessage(e));
                }
            }
        }
        return errors;
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Collection<Pair<Object,MigrationApi.MigrationException>> applyMappings(MigrationBundle bundle, Map<EntityHeader, EntityOperation> entities) {
        Collection<Pair<Object,MigrationApi.MigrationException>> errors = new HashSet<Pair<Object, MigrationApi.MigrationException>>();

        for (EntityHeader header : entities.keySet()) {
            EntityOperation eo = entities.get(header);

            MigrationMetadata metadata = bundle.getMetadata();
            try {
                for (MigrationMapping mapping : metadata.getMappingsForSource(header)) {
                    PropertyResolver resolver = getResolver(eo.entity, mapping.getPropName());
                    EntityOperation targetEo = entities.get(metadata.getHeader(mapping.getDependency()));
                    if (targetEo == null || targetEo.entity == null) {
                        errors.add(new Pair<Object, MigrationApi.MigrationException>(
                            EntityHeaderUtils.fromEntity(eo.entity),
                            new MigrationApi.MigrationException("Cannot apply mapping, target entity not found for dependency reference: " + mapping.getDependency())));
                        continue;
                    }
                    resolver.applyMapping(eo.entity, mapping.getPropName(), targetEo.entity, metadata.getOriginalHeader(mapping.getSourceDependency()));
                }
            } catch (PropertyResolverException e) {
                logger.log(Level.WARNING, "Errors while applying mapping.", e);
                errors.add(new Pair<Object, MigrationApi.MigrationException>(EntityHeaderUtils.fromEntity(eo.entity), new MigrationApi.MigrationException(e)));
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

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Map<EntityHeader,EntityOperation> loadEntities(MigrationBundle bundle, boolean overwriteExisting, boolean enableServices, final Collection<String> errors) {

        Map<EntityHeader, EntityOperation> result = new HashMap<EntityHeader, EntityOperation>();

        Entity existing, fromBundle;
        ExportedItem item;
        MigrationMetadata metadata = bundle.getMetadata();
        for (EntityHeader header : metadata.getHeaders()) {
            try {
                existing = loadEntity(header);
            } catch (MigrationApi.MigrationException e) {
                existing = null;
            }

            item = bundle.getExportedItem(existing == null || metadata.getUnMapped(header) == null ? header : metadata.getUnMapped(header));
            fromBundle = item == null ? null : item.getValue();

            if (fromBundle == null && existing == null) {
                errors.add("Entity not found for header (unresolved mapping not validated?):" + header);
                continue;
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
                if (fromBundle instanceof PersistentEntity && existing instanceof PersistentEntity) {
                    ((PersistentEntity)fromBundle).setOid(((PersistentEntity)existing).getOid());
                    ((PersistentEntity)fromBundle).setVersion(((PersistentEntity)existing).getVersion());
                }
                if (fromBundle instanceof PublishedService && existing instanceof PublishedService) {
                    ((PublishedService)fromBundle).getPolicy().setOid(((PublishedService)existing).getPolicy().getOid());
                    ((PublishedService)fromBundle).getPolicy().setVersion(((PublishedService)existing).getPolicy().getVersion());
                }
                result.put(header, new EntityOperation(fromBundle, UPDATE));
            } else {
                result.put(header, new EntityOperation(existing, IGNORE));
            }
        }

        // add remaining exported items, needed to apply mappings on entities that are updated
        for (ExportedItem exportedItem : bundle.getExportedItems()) {
            EntityHeader header = metadata.getOriginalHeader(exportedItem.getHeaderRef());
            if (header != null && ! result.containsKey(header))
                result.put(header, new EntityOperation(exportedItem.getValue(), IGNORE));
        }

        return result;
    }

    private PropertyResolver getResolver(Entity sourceEntity, String propName) throws PropertyResolverException {

        PropertyResolver annotatedResolver = MigrationUtils.getResolver(sourceEntity, propName);

        if (annotatedResolver != null && ! annotatedResolver.getClass().equals(DefaultEntityPropertyResolver.class))
            return annotatedResolver;
        else
            return resolverFactory.getPropertyResolver(MigrationUtils.getTargetType(sourceEntity, propName));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Collection<String> validateBundle(MigrationBundle bundle) {
        logger.log(Level.FINEST, "Validating bundle: {0}", bundle);
        Collection<String> errors = new HashSet<String>();
        MigrationMetadata metadata = bundle.getMetadata();

        // check that entity values are available for all headers, either in the bundle or already on the SSG
        for (EntityHeader header : metadata.getHeaders()) {
            if (! bundle.hasItem(header)) {
                try {
                    if (loadEntity(header) == null)
                        errors.add("Null entity retrived for header: " + header);
                } catch (MigrationApi.MigrationException e) {
                    errors.add(ExceptionUtils.getMessage(e));
                }
            }
        }

        // dependency-check covered by the above and mapping targets check below

        // mapping requirements                                                                                                                                s
        for(MigrationMapping mapping : metadata.getMappings()) {

            // all headers present in the metadata
            EntityHeaderRef header = mapping.getDependant();
            if (! metadata.hasHeader(header))
                errors.add("Header listed as the source of a dependency, but not included in bundle metadata: " + header);
            header = mapping.getDependency();
            if (! metadata.hasHeader(header))
                errors.add("Header listed as a dependency, but not included in bundle metadata: " + header);

            // name-mapping required
            if (mapping.getType().getNameMapping() == REQUIRED && ! mapping.isMappedDependency())
                errors.add("Unresolved name-mapping: " + mapping);

            // value-mapping required
            if (! mapping.isMappedDependency() && mapping.getType().getValueMapping() == REQUIRED &&
                (bundle.getExportedItem(mapping.getDependency()) == null || ! bundle.getExportedItem(mapping.getDependency()).isMappedValue()) ) {
                errors.add("Unresolved value-mapping: " + mapping);
            }
        }

        return errors;
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Collection<String> checkServiceResolution(Map<EntityHeader, EntityOperation> entities) {
        Collection<String> errors = new HashSet<String>();
        for (EntityHeader header : entities.keySet()) {
            if (header.getType() == EntityType.SERVICE &&
                ( entities.get(header).operation == CREATE ||
                  entities.get(header).operation == UPDATE ) ) {
                try {
                    PublishedService service = (PublishedService) entities.get(header).entity;
                    service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(findServiceDocuments(header, entities)) );
                    resolutionManager.checkDuplicateResolution( service );
                } catch (DuplicateObjectException e) {
                    errors.add("Service resolution error: " + ExceptionUtils.getMessage(e));
                } catch (ServiceResolutionException e) {
                    errors.add("Error getting service resolution parametes: " + ExceptionUtils.getMessage(e));
                }
            }
        }
        return errors;
    }

    private Collection<ServiceDocument> findServiceDocuments( final EntityHeader serviceHeader, final Map<EntityHeader, EntityOperation> entities  ) {
        Collection<ServiceDocument> serviceDocuments = new ArrayList<ServiceDocument>();

        for (EntityHeader header : entities.keySet()) {
            if (header.getType() == EntityType.SERVICE_DOCUMENT ) {
                ServiceDocument serviceDocument = (ServiceDocument) entities.get(header).entity;
                if ( serviceDocument.getServiceId() == serviceHeader.getOid()  ) {
                    serviceDocuments.add( serviceDocument );
                }
            }
        }

        return serviceDocuments;
    }

    private void findDependenciesRecursive(MigrationMetadata result, EntityHeader header) throws MigrationApi.MigrationException, PropertyResolverException {
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
                deps = resolver.getDependencies(header, entity, method);
                for (EntityHeader depHeader : deps.keySet()) {
                    EntityHeader resolvedDepHeader  = resolveHeader( depHeader );
                    for ( MigrationMapping mapping : deps.get(depHeader) ) {
                        if ( depHeader instanceof GuidEntityHeader ) {
                            // TODO find a better way to do this
                            if ( mapping.getDependant().getStrId().equals(((GuidEntityHeader)depHeader).getGuid()) ) {
                                mapping.setDependant( EntityHeaderRef.fromOther(resolvedDepHeader) );
                            }
                            if ( mapping.getDependency().getStrId().equals(((GuidEntityHeader)depHeader).getGuid()) ) {
                                mapping.setSourceDependency( EntityHeaderRef.fromOther(resolvedDepHeader) );
                            }
                        }
                        result.addMapping(mapping);
                        logger.log(Level.FINE, "Added mapping: " + mapping);
                        if ( !result.hasHeader( resolvedDepHeader ) ) {
                            findDependenciesRecursive( result, resolvedDepHeader );
                        }
                    }
                }
            }
        }
    }

    private EntityHeader resolveHeader( final EntityHeader header ) throws MigrationApi.MigrationException {
        Entity ent = loadEntity(header);
        return ent != null ? EntityHeaderUtils.fromEntity(ent) : header;
    }

    private Collection<EntityHeader> resolveHeaders( final Collection<EntityHeader> headers ) throws MigrationApi.MigrationException {
        List<EntityHeader> resolvedHeaders = new ArrayList<EntityHeader>( headers.size() );

        for ( EntityHeader header : headers ) {
            resolvedHeaders.add( resolveHeader( header ) );
        }

        return resolvedHeaders;
    }

    private String composeErrorMessage( final String summary, final EntityHeader header, final Exception root ) {
        String message = summary + ":\n";
        if ( header instanceof ServiceHeader) {
            message += header.getType() +", " + ((ServiceHeader)header).getDisplayName() + " (#"+header.getOid()+")\ndue to:\n";
        } else {
            message += header.getType() +", " + (header.getName()==null? "" : header.getName()) + " (#"+header.getOid()+")\ndue to:\n";
        }
        message += ExceptionUtils.getMessage(root);
        return message;
    }

    private Entity loadEntity( final EntityHeader header ) throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Loading entity for header: {0}", header);
        try {
            Entity ent = entityCrud.find(header); // load the entity
            if (ent == null)
                throw new MigrationApi.MigrationException("Error loading the entity for header "+ header.getType() +", " + (header.getName()==null? "" : header.getName()) + " (#"+header.getOid()+")");
            return ent;
        } catch (FindException e) {
            throw new MigrationApi.MigrationException("Error loading the entity for header: " + header, e);
        }
    }
}
