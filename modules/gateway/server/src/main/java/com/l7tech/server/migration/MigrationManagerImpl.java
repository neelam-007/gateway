package com.l7tech.server.migration;

import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.ExportedItem;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import static com.l7tech.server.management.migration.bundle.MigratedItem.ImportOperation.*;
import com.l7tech.server.management.migration.MigrationManager;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.*;
import com.l7tech.server.*;
import com.l7tech.server.service.resolution.ResolutionManager;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.util.ExceptionUtils;
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
    private final static String ROOT_FOLDER_OID = "-5002";
    private ResolutionManager resolutionManager;
    private ExternalEntityHeader rootFolderHeader;

    public MigrationManagerImpl(EntityCrud entityCrud, PropertyResolverFactory resolverFactory, ResolutionManager resolutionManager) {
        this.entityCrud = entityCrud;
        this.resolverFactory = resolverFactory;
        this.resolutionManager = resolutionManager;
    }

    @Override
    public Collection<ExternalEntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationApi.MigrationException {
        if ( clazz == null ) throw new MigrationApi.MigrationException("Missing required parameter.");
        logger.log(Level.FINEST, "Listing entities for class: {0}", clazz.getName());
        try {
            return EntityHeaderUtils.toExternal(entityCrud.findAll(clazz));
        } catch (FindException e) {
            throw new MigrationApi.MigrationException("Error listing entities for " + clazz + " : " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    @Override
    public Collection<ExternalEntityHeader> checkHeaders(Collection<ExternalEntityHeader> headers) {
        Collection<ExternalEntityHeader> result = new HashSet<ExternalEntityHeader>();

        if ( headers != null ) {
            for (ExternalEntityHeader header : headers) {
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
    public MigrationMetadata findDependencies(Collection<ExternalEntityHeader> headers ) throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Finding dependencies for headers: {0}", headers);
        MigrationMetadata result = new MigrationMetadata();

        if ( headers != null ) {
            Set<ExternalEntityHeader> resolvedHeaders = resolveHeaders(headers);
            result.setHeaders( new LinkedHashSet<ExternalEntityHeader>(resolvedHeaders));

            for ( ExternalEntityHeader header : resolvedHeaders ) {
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
    public MigrationBundle exportBundle(final Collection<ExternalEntityHeader> headers) throws MigrationApi.MigrationException {
        if ( headers == null || headers.isEmpty() ) throw new MigrationApi.MigrationException("Missing required parameter.");        
        MigrationMetadata metadata = findDependencies(headers);
        MigrationBundle bundle = new MigrationBundle(metadata);
        for (ExternalEntityHeader header : metadata.getHeaders()) {
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
    public Map<ExternalEntityHeader, EntityHeaderSet<ExternalEntityHeader>> retrieveMappingCandidates(Collection<ExternalEntityHeader> mappables, String filter) throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Retrieving mapping candidates for {0}.", mappables);
        Map<ExternalEntityHeader, EntityHeaderSet<ExternalEntityHeader>> result = new HashMap<ExternalEntityHeader, EntityHeaderSet<ExternalEntityHeader>>();

        if ( mappables != null ) {
            for (ExternalEntityHeader header : mappables) {
                try {
                    EntityHeaderSet<ExternalEntityHeader> candidates = new EntityHeaderSet<ExternalEntityHeader>();
                    if (header instanceof ValueReferenceEntityHeader) {
                        // special handling for value reference headers
                        MigrationMetadata metadata = findDependencies(EntityHeaderUtils.toExternal(
                            entityCrud.findAll(EntityTypeRegistry.getEntityClass(((ValueReferenceEntityHeader)header).getOwnerType()), filter, 0, 50)));
                        for (ExternalEntityHeader maybeCandidate : metadata.getHeaders()) {
                            if (maybeCandidate instanceof ValueReferenceEntityHeader)
                                candidates.add(maybeCandidate);
                        }
                    } else {
                        for (EntityHeader candidate : entityCrud.findAll(EntityHeaderUtils.getEntityClass(header), filter, 0, 50 )) {
                            candidates.add(resolveHeader(EntityHeaderUtils.toExternal(candidate)));
                        }
                    }
                    logger.log(Level.FINEST, "Found {0} mapping candidates for header {1}.", new Object[]{candidates.size(), header});
                    result.put(header, candidates);
                } catch (FindException e) {
                    throw new MigrationApi.MigrationException("Error retrieving mapping candidate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }

        return result;
    }

    @Override
    public Collection<MigratedItem> importBundle(MigrationBundle bundle, ExternalEntityHeader targetFolder,
                                                 boolean flattenFolders, boolean overwriteExisting, boolean enableServices, boolean dryRun) throws MigrationApi.MigrationException {

        MigrationMetadata metadata = bundle.getMetadata();

        Collection<String> errors = processFolders(bundle, targetFolder, flattenFolders);

        Map<ExternalEntityHeader,Entity> entitiesFromTarget = loadMappedEntities(metadata);

        errors.addAll(validateBundle(bundle, entitiesFromTarget));

        if (!errors.isEmpty())
            logger.log(Level.WARNING, "Bundle validation errors: {0}.", errors);
            //throw new MigrationApi.MigrationException(errors);

        Map<ExternalEntityHeader, MigratedItem> result = new HashMap<ExternalEntityHeader, MigratedItem>();
        for(ExternalEntityHeader header : metadata.getHeaders()) {
            try {
                upload(header, bundle, entitiesFromTarget, result, overwriteExisting, enableServices, dryRun, false);
            } catch ( ObjectModelException ome ) {
                throw new MigrationApi.MigrationException(composeErrorMessage("Import failed when processing entity", header, ome));
            }
        }

        return result.values();
    }

    private Map<ExternalEntityHeader,Entity> loadMappedEntities(MigrationMetadata metadata) throws MigrationApi.MigrationException {
        Map<ExternalEntityHeader,Entity> entitiesFromTarget = new HashMap<ExternalEntityHeader, Entity>();

        for(ExternalEntityHeader header : metadata.getMappedHeaders()) {
            entitiesFromTarget.put(header, loadEntity(header));
        }
        for(ExternalEntityHeader header : metadata.getCopiedHeaders()) {
            entitiesFromTarget.put(header, loadEntity(header));
        }

        return entitiesFromTarget;
    }


    private void upload(ExternalEntityHeader header, MigrationBundle bundle, Map<ExternalEntityHeader, Entity> entitiesFromTarget, Map<ExternalEntityHeader, MigratedItem> result,
                        boolean overwriteExisting, boolean enableServices, boolean dryRun, boolean isRecursing)
        throws MigrationApi.MigrationException, UpdateException, SaveException {

        MigrationMetadata metadata = bundle.getMetadata();

        if (result.containsKey(header)) {
            if (isRecursing && ! metadata.hasHeader(header))
                logger.log(Level.WARNING, "Circular dependency reached during entity upload for header {0}.", header);
            return;
        }

        // determine the upload operation
        MigratedItem.ImportOperation op = metadata.wasCopied(header) ?
            overwriteExisting ? UPDATE : IGNORE :
            metadata.isMapped(header) ? IGNORE : CREATE;

        ExternalEntityHeader targetHeader;
        Entity entity;

        if (op != IGNORE) {
            // upload dependencies first
            for (MigrationDependency dep : metadata.getDependencies(header)) {
                upload(dep.getDependency(), bundle, entitiesFromTarget, result, overwriteExisting, enableServices, dryRun, true);
            }
        }

        // do upload
        switch (op) {
            case IGNORE:
                targetHeader = metadata.getCopiedOrMapped(header);
                entity = entitiesFromTarget.get(targetHeader);
                if (entity == null)
                    throw new MigrationApi.MigrationException("Entity not found on the target cluster for header: " + header);
                break;

            case UPDATE:
                targetHeader = metadata.getCopiedOrMapped(header);
                entity = bundle.getExportedEntity(header);
                if (entity == null)
                    throw new MigrationApi.MigrationException("Entity not found in the bundle for header: " + header);
                if (header instanceof ValueReferenceEntityHeader)
                    break;
                Entity onTarget = entitiesFromTarget.get(metadata.getCopied(header));
                if (entity instanceof PersistentEntity && onTarget instanceof PersistentEntity) {
                    ((PersistentEntity) entity).setOid(((PersistentEntity)onTarget).getOid());
                    ((PersistentEntity) entity).setVersion(((PersistentEntity)onTarget).getVersion());
                }
                if (entity instanceof PublishedService && onTarget instanceof PublishedService) {
                    ((PublishedService)entity).getPolicy().setOid(((PublishedService)onTarget).getPolicy().getOid());
                    ((PublishedService)entity).getPolicy().setVersion(((PublishedService)onTarget).getPolicy().getVersion());
                }
                checkServiceResolution(header, entity, bundle);
                if (!dryRun)
                    entityCrud.update(entity);
                break;

            case CREATE:
                targetHeader = header;
                entity = bundle.getExportedEntity(header);
                if (entity == null)
                    throw new MigrationApi.MigrationException("Entity not found in the bundle for header: " + header);
                if (header instanceof ValueReferenceEntityHeader)
                    break;
                if (!enableServices && entity instanceof PublishedService)
                    ((PublishedService) entity).setDisabled(true);
                checkServiceResolution(header, entity, bundle);
                // todo: warn early (at dry-run) about entity identity conflicts
                if (!dryRun) {
                    Long oid = (Long) entityCrud.save(entity);
                    if (entity instanceof PersistentEntity)
                        ((PersistentEntity) entity).setOid(oid);
                }
                break;

            default:
                throw new IllegalStateException("Import operation not known: " + op);
        }

        if (! (header instanceof ValueReferenceEntityHeader) )
            result.put(header, new MigratedItem(header, EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(entity)), op));

        try {
            // apply dependency value to dependants
            for (MigrationDependency dep : metadata.getDependants(header)) {
                ExternalEntityHeader dependant = dep.getDependant();
                Entity dependantEntity = metadata.isMapped(dependant) ? entitiesFromTarget.get(metadata.getMapping(dependant)) : bundle.getExportedEntity(dependant);

                PropertyResolver resolver = getResolver(dependantEntity, dep.getPropName());
                if (entity == null) {
                    throw new MigrationApi.MigrationException("Cannot apply mapping, target entity not found for dependency reference: " + dep.getDependency());
                }
                resolver.applyMapping(dependantEntity, dep.getPropName(), targetHeader, entity, header);
            }
        } catch (PropertyResolverException e) {
            throw new MigrationApi.MigrationException(e);
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Collection<String> processFolders(MigrationBundle bundle, ExternalEntityHeader targetFolder, boolean flatten) throws MigrationApi.MigrationException {
        Collection<String> errors = new HashSet<String>();
        MigrationMetadata metadata = bundle.getMetadata();
        if (flatten) {
            // replace all folder dependencies with the (unique) targetFolder
            Set<ExternalEntityHeader> headersToRemove = new HashSet<ExternalEntityHeader>();
            for (MigrationDependency dep : metadata.getDependencies()) {
                if (dep.getDependency().getType() == EntityType.FOLDER) {
                    headersToRemove.add(dep.getDependency());
                    metadata.addMappingOrCopy(dep.getDependant(), targetFolder, false);
                }
            }
            for (ExternalEntityHeader header : headersToRemove) {
                metadata.removeHeader(header);
            }
            metadata.addHeader(targetFolder);
        } else {
            // map root folder to target folder
            if (!metadata.hasHeader(getRootFolderHeader())) {
                logger.log(Level.INFO, "Root folder not found in the bundle, not processing folders.");
            } else {
                metadata.addMappingOrCopy(getRootFolderHeader(), targetFolder, false);
            }
        }
        return errors;
    }

    private ExternalEntityHeader getRootFolderHeader() throws MigrationApi.MigrationException {
        if (rootFolderHeader == null) {
            try {
                rootFolderHeader = EntityHeaderUtils.toExternal(entityCrud.findHeader(EntityType.FOLDER, ROOT_FOLDER_OID));
            } catch (FindException e) {
                throw new MigrationApi.MigrationException("Error getting root folder header.", e);
            }
        }

        return rootFolderHeader;
    }

    private PropertyResolver getResolver(Entity sourceEntity, String propName) throws PropertyResolverException {
        return resolverFactory.getPropertyResolver(MigrationUtils.getTargetType(sourceEntity, propName));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Collection<String> validateBundle(MigrationBundle bundle, Map<ExternalEntityHeader, Entity> mappedEntities) {
        logger.log(Level.FINEST, "Validating bundle: {0}", bundle);
        Collection<String> errors = new HashSet<String>();
        MigrationMetadata metadata = bundle.getMetadata();

        // check that entity values are available for all headers, either in the bundle or already on the SSG
        for (ExternalEntityHeader header : metadata.getHeaders()) {
            if (! bundle.hasItem(header) && mappedEntities.get(bundle.getMetadata().getMapping(header)) == null) {
                errors.add("Entity not found for header: " + header);
            }
        }

        // dependency-check covered by the above and mapping targets check below

        // mapping requirements
        for(MigrationDependency dep : metadata.getDependencies()) {

            // all headers present in the metadata
            ExternalEntityHeader header = dep.getDependant();
            if (! metadata.hasHeader(header))
                errors.add("Header listed as the source of a dependency, but not included in bundle metadata: " + header);
            header = dep.getDependency();
            if (! metadata.hasHeader(header))
                errors.add("Header listed as a dependency, but not included in bundle metadata: " + header);

            // name-mapping required
            if (dep.getMappingType().getNameMapping() == MigrationMappingSelection.REQUIRED && ! metadata.isMapped(header))
                errors.add("Unresolved name-mapping: " + dep);

            // value-mapping required
            if (! metadata.isMapped(header) && dep.getMappingType().getValueMapping() == MigrationMappingSelection.REQUIRED &&
                (bundle.getExportedItem(dep.getDependency()) == null || ! bundle.getExportedItem(dep.getDependency()).isMappedValue()) ) {
                errors.add("Unresolved value-mapping: " + dep);
            }
        }

        return errors;
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Collection<String> checkServiceResolution(ExternalEntityHeader header, Entity entity, MigrationBundle bundle) {
        Collection<String> errors = new HashSet<String>();
        if (header.getType() == EntityType.SERVICE) {
            try {
                PublishedService service = (PublishedService) entity;
                service.parseWsdlStrategy(new ServiceDocumentWsdlStrategy(findServiceDocuments(header, bundle.getExportedEntities())));
                resolutionManager.checkDuplicateResolution(service);
            } catch (DuplicateObjectException e) {
                errors.add("Service resolution error: " + ExceptionUtils.getMessage(e));
            } catch (ServiceResolutionException e) {
                errors.add("Error getting service resolution parametes: " + ExceptionUtils.getMessage(e));
            }
        }
        return errors;
    }

    private Collection<ServiceDocument> findServiceDocuments( final ExternalEntityHeader serviceHeader, final Map<ExternalEntityHeader, Entity> entities  ) {
        Collection<ServiceDocument> serviceDocuments = new ArrayList<ServiceDocument>();

        for (ExternalEntityHeader header : entities.keySet()) {
            if (header.getType() == EntityType.SERVICE_DOCUMENT ) {
                ServiceDocument serviceDocument = (ServiceDocument) entities.get(header);
                if ( serviceDocument.getServiceId() == serviceHeader.getOid()  ) {
                    serviceDocuments.add( serviceDocument );
                }
            }
        }

        return serviceDocuments;
    }

    private void findDependenciesRecursive(MigrationMetadata result, ExternalEntityHeader header) throws MigrationApi.MigrationException, PropertyResolverException {
        logger.log(Level.FINE, "Finding dependencies for: " + header.toStringVerbose());

        Entity entity = null;
        try {
            entity = loadEntity(header);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error loading entity for dependency '"+header+"'.", ExceptionUtils.getDebugException(e));
        }
        result.addHeader(header); // marks header as processed
        if (entity == null || header instanceof ValueReferenceEntityHeader)
            return;

        for (Method method : entity.getClass().getMethods()) {
            if (MigrationUtils.isDependency(method)) {
                PropertyResolver resolver = getResolver(entity, method.getName());
                Map<ExternalEntityHeader, Set<MigrationDependency>> deps;
                deps = resolver.getDependencies(header, entity, method, MigrationUtils.propertyNameFromGetter(method.getName()));
                for (ExternalEntityHeader depHeader : deps.keySet()) {
                    ExternalEntityHeader resolvedDepHeader = resolveHeader(depHeader);
                    for ( MigrationDependency dependency : deps.get(depHeader) ) {
                        dependency.setDependency(resolvedDepHeader);
                        result.addDependency(dependency);
                        logger.log(Level.FINE, "Added dependency: " + dependency);
                        if ( !result.hasHeader( resolvedDepHeader ) ) {
                            findDependenciesRecursive( result, resolvedDepHeader );
                        }
                    }
                }
            }
        }
    }

    private ExternalEntityHeader resolveHeader(final ExternalEntityHeader header) throws MigrationApi.MigrationException {
        if (header instanceof ValueReferenceEntityHeader)
            return header;
        Entity ent = loadEntity(header);
        return ent == null ? header : EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(ent));
    }

    private Set<ExternalEntityHeader> resolveHeaders(final Collection<ExternalEntityHeader> headers) throws MigrationApi.MigrationException {
        Set<ExternalEntityHeader> resolvedHeaders = new LinkedHashSet<ExternalEntityHeader>(headers.size());

        for (ExternalEntityHeader header : headers) {
            resolvedHeaders.add(resolveHeader(header));
        }

        return resolvedHeaders;
    }

    private String composeErrorMessage( final String summary, final ExternalEntityHeader externalHeader, final Exception root ) {
        EntityHeader header = EntityHeaderUtils.fromExternal(externalHeader);
        String message = summary + ":\n";
        if ( header instanceof ServiceHeader) {
            message += header.getType() +", " + ((ServiceHeader)header).getDisplayName() + " (#"+header.getOid()+")\ndue to:\n";
        } else {
            message += header.getType() +", " + (header.getName()==null? "" : header.getName()) + " (#"+header.getOid()+")\ndue to:\n";
        }
        message += ExceptionUtils.getMessage(root);
        return message;
    }

    private Entity loadEntity( final ExternalEntityHeader externalHeader ) throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Loading entity for header: {0}", externalHeader);
        EntityHeader header = EntityHeaderUtils.fromExternal(externalHeader);
        Entity ent;
        try {
            // special handling for value-reference entities
            if (header instanceof ValueReferenceEntityHeader) {
                ValueReferenceEntityHeader vrHeader = (ValueReferenceEntityHeader) header;
                ent = entityCrud.find(EntityTypeRegistry.getEntityClass(vrHeader.getOwnerType()), vrHeader.getOwnwerId());
            } else {
                ent = entityCrud.find(header); // load the entity
            }
        } catch (Exception e) {
            throw new MigrationApi.MigrationException("Error loading the entity for header: " + header, e);
        }
        if (ent == null)
            throw new MigrationApi.MigrationException("Error loading the entity for header "+ header.getType() +", " + (header.getName()==null? "" : header.getName()) + " (#"+header.getOid()+")");
        return ent;
    }
}
