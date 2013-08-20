package com.l7tech.server.migration;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.*;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.cluster.ExternalEntityHeaderEnhancer;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.MigrationManager;
import com.l7tech.server.management.migration.bundle.ExportedItem;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.util.ExceptionUtils;
import org.springframework.transaction.annotation.Transactional;

import java.io.Flushable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.management.migration.bundle.MigratedItem.ImportOperation.*;

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
    private ExternalEntityHeaderEnhancer externalEntityHeaderEnhancer;

    public MigrationManagerImpl( final EntityCrud entityCrud,
                                 final PropertyResolverFactory resolverFactory,
                                 final ExternalEntityHeaderEnhancer externalEntityHeaderEnhancer ) {
        this.entityCrud = entityCrud;
        this.resolverFactory = resolverFactory;
        this.externalEntityHeaderEnhancer = externalEntityHeaderEnhancer;
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
                    Entity entity = loadEntity(header);
                    ExternalEntityHeader externalHeader = entity == null ? header : EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(entity));
                    enhanceHeader( externalHeader );
                    result.add( externalHeader );
                } catch (MigrationApi.MigrationException e) {
                    logger.log(Level.FINE, "Not validating header: entity not found: {0}", header);
                }
            }
        }
        return result;
    }
    
    @Override
    public MigrationMetadata findDependencies(Collection<ExternalEntityHeader> headers ) throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Finding dependencies for headers: {0}", headers);
        MigrationMetadata metadata = new MigrationMetadata();

        if ( headers != null ) {
            Set<ExternalEntityHeader> resolvedHeaders = resolveHeaders(headers);
            metadata.setHeaders( new LinkedHashSet<ExternalEntityHeader>(resolvedHeaders));

            for ( ExternalEntityHeader header : resolvedHeaders ) {
                try {
                    Set<ExternalEntityHeader> visited = new HashSet<ExternalEntityHeader>(metadata.getHeaders());
                    findDependenciesRecursive(metadata, header, visited);
                } catch (PropertyResolverException e) {
                    throw new MigrationApi.MigrationException(composeErrorMessage("Error when processing entity", header, e));
                } catch (MigrationApi.MigrationException e) {
                    throw new MigrationApi.MigrationException(composeErrorMessage("Error when processing entity", header, e));
                }

                if (EntityType.SERVICE == header.getType()) {
                    for (MigrationDependency dep : metadata.getDependants(header)) {
                        ExternalEntityHeader maybeDocument = dep.getDependant();
                        if (EntityType.SERVICE_DOCUMENT == maybeDocument.getType()) {
                            metadata.addHeader(maybeDocument);
                        }
                    }
                }
            }
        }

        return metadata;
    }

    @Override
    public MigrationBundle exportBundle(final Collection<ExternalEntityHeader> headers) throws MigrationApi.MigrationException {
        if ( headers == null || headers.isEmpty() ) throw new MigrationApi.MigrationException("Missing required parameter.");        
        MigrationMetadata metadata = findDependencies(headers);
        MigrationBundle bundle = new MigrationBundle(metadata);
        for (ExternalEntityHeader header : metadata.getAllHeaders()) {
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
    public Map<ExternalEntityHeader, EntityHeaderSet<ExternalEntityHeader>> retrieveMappingCandidates(
                        Collection<ExternalEntityHeader> mappables, ExternalEntityHeader scope, final Map<String,String> filters)
                        throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Retrieving mapping candidates for {0}.", mappables);
        Map<ExternalEntityHeader, EntityHeaderSet<ExternalEntityHeader>> result = new HashMap<ExternalEntityHeader, EntityHeaderSet<ExternalEntityHeader>>();

        Map<String,String> customFilters;
        if ( mappables != null ) {
            for (ExternalEntityHeader header : mappables) {
                try {
                    customFilters = getCustomFilters(header, filters);
                    EntityHeaderSet<ExternalEntityHeader> candidates = new EntityHeaderSet<ExternalEntityHeader>();
                    if (header instanceof ValueReferenceEntityHeader) {
                        // special handling for value reference headers
                        MigrationMetadata metadata = findDependencies(EntityHeaderUtils.toExternal(
                            entityCrud.findAll(EntityTypeRegistry.getEntityClass(((ValueReferenceEntityHeader)header).getOwnerType()), customFilters, 0, 50)));
                        for (ExternalEntityHeader maybeCandidate : metadata.getAllHeaders()) {
                            if (maybeCandidate instanceof ValueReferenceEntityHeader)
                                candidates.add(maybeCandidate);
                        }
                    } else {
                        for (EntityHeader candidate : entityCrud.findAllInScope(EntityHeaderUtils.getEntityClass(header), scope, customFilters, 0, 50 )) {
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

    private Map<String, String> getCustomFilters(ExternalEntityHeader header, Map<String, String> filters) {
        if (EntityType.JMS_ENDPOINT == header.getType() && header.getExtraProperties().containsKey("messageSource")) {
            Map<String,String> customFilters = new HashMap<String, String>();
            customFilters.put("messageSource", header.getProperty("messageSource"));
            customFilters.putAll(filters);
            return customFilters;
        } else if (EntityType.SSG_ACTIVE_CONNECTOR == header.getType() && (header.getExtraProperties().containsKey("connectorType") || header.getExtraProperties().containsKey("inbound"))) {
            Map<String,String> customFilters = new HashMap<String, String>();
            if ( header.getExtraProperties().containsKey("connectorType") ) customFilters.put("type", header.getProperty("connectorType"));
            if ( header.getExtraProperties().containsKey("inbound") ) customFilters.put("properties['inbound']", header.getProperty("inbound"));
            customFilters.putAll(filters);
            return customFilters;
        } else if ( EntityType.RESOURCE_ENTRY == header.getType() && header.getExtraProperties().containsKey("resourceType") ) {
            Map<String,String> customFilters = new HashMap<String, String>();
            customFilters.put("type", header.getProperty("resourceType"));
            customFilters.putAll(filters);
            return customFilters;
        } else {
            return filters;
        }
    }

    @Override
    public Collection<MigratedItem> importBundle(MigrationBundle bundle, boolean dryRun) throws MigrationApi.MigrationException {

        MigrationMetadata metadata = bundle.getMetadata();

        Collection<String> errors = processFolders(bundle, metadata.getTargetFolder(), ! metadata.isMigrateFolders());

        Map<ExternalEntityHeader,Entity> entitiesFromTarget = loadEntitiesFromTarget(metadata);

        errors.addAll(validateBundle(bundle, entitiesFromTarget));

        if (!errors.isEmpty())
            throw new MigrationApi.MigrationException(errors);

        Map<ExternalEntityHeader, MigratedItem> result = new HashMap<ExternalEntityHeader, MigratedItem>();
        for(ExternalEntityHeader header : metadata.getHeaders()) {
            try {
                upload(header, bundle, entitiesFromTarget, result, metadata.isOverwrite(), metadata.isEnableNewServices(), dryRun, false);
            } catch ( ObjectModelException ome ) {
                throw new MigrationApi.MigrationException(composeErrorMessage("Import failed when processing entity", header, ome));
            }
        }

        return result.values();
    }

    private Map<ExternalEntityHeader,Entity> loadEntitiesFromTarget(MigrationMetadata metadata) throws MigrationApi.MigrationException {
        Map<ExternalEntityHeader,Entity> entitiesFromTarget = new HashMap<ExternalEntityHeader, Entity>();

        Map<ExternalEntityHeader,Boolean> headersToLoad = new HashMap<ExternalEntityHeader, Boolean>();

        for(ExternalEntityHeader header : metadata.getAllHeaders()) {
            // top-level selection and dependencies
            if (! metadata.isMapped(header))
                headersToLoad.put(header, false);
        }
        for(ExternalEntityHeader header : metadata.getMappedHeaders()) {
            headersToLoad.put(header, true);
        }
        for(ExternalEntityHeader header : metadata.getCopiedHeaders()) {
            headersToLoad.put(header, true);
        }

        Entity ent;
        for(ExternalEntityHeader header : headersToLoad.keySet()) {
            try {
                ent = loadEntity(header);
                entitiesFromTarget.put(header, ent);
                entitiesFromTarget.put(EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(ent)), ent);
            } catch (MigrationApi.MigrationException e) {
                if (headersToLoad.get(header)) throw e; // header must be present on target cluster
            }
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
        MigratedItem.ImportOperation op;
        ExternalEntityHeader targetHeader;
        if (metadata.isMapped(header)) {
            op = MAP;
            targetHeader = getUpdatedHeader(metadata.getCopiedOrMapped(header));
        } else if ( ! metadata.wasCopied(header) && ! entitiesFromTarget.containsKey(header)) {
            op = CREATE;
            targetHeader = header;
        } else if (! overwriteExisting || ! bundle.hasValueForHeader(header) || EntityType.FOLDER == header.getType()) {
            op = MAP_EXISTING;
            targetHeader = metadata.wasCopied(header) ? getUpdatedHeader(metadata.getCopiedOrMapped(header)) : header;
        } else if (metadata.wasCopied(header) && entitiesFromTarget.containsKey(metadata.getCopied(header))) {
            op = UPDATE;
            targetHeader = metadata.getCopied(header);
        } else {
            op = OVERWRITE;
            targetHeader = metadata.wasCopied(header) ? getUpdatedHeader(metadata.getCopiedOrMapped(header)) : header;
        }

        if (op.modifiesTarget()) {
            for (MigrationDependency dep : metadata.getDependencies(header)) {
                upload(dep.getDependency(), bundle, entitiesFromTarget, result, overwriteExisting, enableServices, dryRun, true);
            }
        }

        // do upload
        Entity entity;
        switch (op) {
            case MAP:
            case MAP_EXISTING:
                entity = entitiesFromTarget.get(targetHeader);
                break;

            case OVERWRITE:
                entity = updateEntity(header, targetHeader, bundle, entitiesFromTarget, dryRun);
                break;

            case UPDATE:
                entity = updateEntity(header, targetHeader, bundle, entitiesFromTarget, dryRun);
                break;

            case CREATE:
                entity = createEntity(header, bundle, enableServices, dryRun);
                break;

            default:
                throw new IllegalStateException("Import operation not known: " + op);
        }

        if (entity == null)
            throw new MigrationApi.MigrationException("Entity not found on the target cluster for header: " + header);

        if (! (header instanceof ValueReferenceEntityHeader) || header.getMappedValue() != null)
            result.put(header, new MigratedItem(header, EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(entity)), op));

        if (header instanceof  ValueReferenceEntityHeader)
            applyValueReference((ValueReferenceEntityHeader) header, bundle);
        else
            applyMappings(header, entity, targetHeader, bundle, entitiesFromTarget);
    }

    private ExternalEntityHeader getUpdatedHeader(ExternalEntityHeader header) throws MigrationApi.MigrationException {
        return EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(loadEntity(header)));
    }

    private void applyValueReference(ValueReferenceEntityHeader vRefHeader, MigrationBundle bundle) throws MigrationApi.MigrationException {
        if (vRefHeader.getMappedValue() != null) {
            Entity sourceEntity = bundle.getExportedEntity(vRefHeader.getOwnerHeader());
            try {
                PropertyResolver resolver = getResolver(sourceEntity, vRefHeader.getPropertyName());
                resolver.applyMapping(sourceEntity, vRefHeader.getPropertyName(), vRefHeader, vRefHeader.getMappedValue(), null);
            } catch (PropertyResolverException e) {
                throw new MigrationApi.MigrationException("Error applying value reference for: " + vRefHeader, e);
            }
        }
    }

    private void applyMappings( ExternalEntityHeader header, Entity entity, ExternalEntityHeader targetHeader,
                                MigrationBundle bundle, Map<ExternalEntityHeader, Entity> entitiesFromTarget ) throws MigrationApi.MigrationException {
        MigrationMetadata metadata = bundle.getMetadata();
        try {
            // apply dependency value to dependants
            for (MigrationDependency dep : metadata.getDependants(header)) {
                ExternalEntityHeader dependant = dep.getDependant();
                Entity dependantEntity = metadata.isMapped(dependant) ? entitiesFromTarget.get(metadata.getMapping(dependant)) : bundle.getExportedEntity(dependant);
                if (dependantEntity == entity) continue;
                String propName = correctPropName(dependantEntity, dep.getPropName());
                PropertyResolver resolver = getResolver(dependantEntity, propName);
                if (entity == null) {
                    throw new MigrationApi.MigrationException("Cannot apply mapping, target entity not found for dependency reference: " + dep.getDependency());
                }
                resolver.applyMapping(dependantEntity, propName, targetHeader, entity, header);
            }
        } catch (PropertyResolverException e) {
            throw new MigrationApi.MigrationException(e);
        }
    }

    /**
     * This will correct the property name in the case the is is a reference to an old oid.
     * @param entity The entity to correct the property name for
     * @param propertyName The property name
     * @return The corrected property name, or the original if it didn't need to be corrected.
     */
    private static String correctPropName(Entity entity, String propertyName) {
        if(entity instanceof GoidEntity){
            switch(propertyName){
                case "EntityOid":
                    return "EntityGoid";
                default:
                    return propertyName;
            }
        } else {
            return propertyName;
        }
    }

    private Entity updateEntity(ExternalEntityHeader header, ExternalEntityHeader targetHeader, MigrationBundle bundle, Map<ExternalEntityHeader, Entity> entitiesFromTarget, boolean dryRun)
        throws MigrationApi.MigrationException, UpdateException {

        if (header instanceof ValueReferenceEntityHeader) return bundle.getExportedEntity(((ValueReferenceEntityHeader)header).getOwnerHeader());
        Entity entity = header.isValueMappable() && header.getMappedValue() != null ? getValueMappedEntity(header, bundle) : bundle.getExportedEntity(header);
        if (entity == null)
            throw new MigrationApi.MigrationException("Entity not found in the bundle for header: " + header);

        Entity onTarget = entitiesFromTarget.get(targetHeader);
        if (entity instanceof GoidEntity && onTarget instanceof GoidEntity) {
            ((GoidEntity) entity).setGoid(((GoidEntity)onTarget).getGoid());
            ((GoidEntity) entity).setVersion(((GoidEntity)onTarget).getVersion());
        }
        if (entity instanceof PublishedService && onTarget instanceof PublishedService) {
            ((PublishedService)entity).getPolicy().setGoid(((PublishedService)onTarget).getPolicy().getGoid());
            ((PublishedService)entity).getPolicy().setVersion(((PublishedService)onTarget).getPolicy().getVersion());
            ((PublishedService)entity).setDisabled(((PublishedService)onTarget).isDisabled());
            ((PublishedService)entity).parseWsdlStrategy( buildWsdlStrategy( header, bundle ) );
        }

        if (entity instanceof Flushable ) {
            try {
                ((Flushable)entity).flush();
            } catch ( IOException e ) {
                throw new MigrationApi.MigrationException("Error flushing entity for update: " + header, e);
            }
        }

        if (!dryRun) {
            entityCrud.update(entity);
            // todo: need more reliable method of retrieving the new version;
            // loadEntity() returns null until the whole import (transactional) completes, version is not always incremented (e.g. if the new entity is not different) 
            if (entity instanceof GoidEntity && onTarget instanceof GoidEntity)
                ((GoidEntity) entity).setVersion(((GoidEntity) onTarget).getVersion() + (entity.equals(onTarget) ? 0 : 1) );
        }

        return entity;
    }

    private Entity createEntity(ExternalEntityHeader header, MigrationBundle bundle, boolean enableServices, boolean dryRun) throws MigrationApi.MigrationException, SaveException {
        if (header instanceof ValueReferenceEntityHeader) return bundle.getExportedEntity(((ValueReferenceEntityHeader)header).getOwnerHeader());
        Entity entity = header.isValueMappable() && header.getMappedValue() != null ? getValueMappedEntity(header, bundle) : bundle.getExportedEntity(header);
        if (entity == null)
            throw new MigrationApi.MigrationException("Entity not found in the bundle for header: " + header);

        if (entity instanceof PublishedService) {
            final PublishedService service = (PublishedService) entity;
            service.parseWsdlStrategy( buildWsdlStrategy( header, bundle ) );
            service.setDisabled( !enableServices );
        }

        if (entity instanceof Flushable ) {
            try {
                ((Flushable)entity).flush();
            } catch ( IOException e ) {
                throw new MigrationApi.MigrationException("Error flushing entity for save: " + header, e);
            }
        }

        if (!dryRun) {
            if (entity instanceof GoidEntity)
                ((GoidEntity) entity).setVersion(0);
            Serializable id = entityCrud.save(entity);
            if(entity instanceof GoidEntity){
                ((GoidEntity) entity).setGoid((Goid)id);
            }
        }
        return entity;
    }


    /**
     * Looks up a property resolver for the given header and uses it to convert the mapped value to an Entity.
     *
     * @param header  Header containing a value-mapping.
     * @param bundle  The migration bundle.
     * @return        Entity converted from the mapped value, or null if there is no value-mapping.
     */
    private Entity getValueMappedEntity(ExternalEntityHeader header, MigrationBundle bundle) throws MigrationApi.MigrationException {
        Object mappedValue = header.getMappedValue();
        if (! header.isValueMappable() || mappedValue == null) return null;

        PropertyResolver tempResolver, resolver = null;
        for (MigrationDependency dep : bundle.getMetadata().getDependants(header)) {
            tempResolver = resolverFactory.getPropertyResolver(dep.getResolverType());
            if (resolver != null && resolver != tempResolver)
                throw new MigrationApi.MigrationException("More than one property resolver found for: " + header);
            resolver = tempResolver;
        }

        if (resolver == null) {
            throw new MigrationApi.MigrationException("No property resolver found for: " + header);
        }
        try {
            return resolver.valueMapping(header);
        } catch (PropertyResolverException e) {
            throw new MigrationApi.MigrationException(e);
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Collection<String> processFolders(MigrationBundle bundle, ExternalEntityHeader targetFolder, boolean flatten) throws MigrationApi.MigrationException {
        Collection<String> errors = new HashSet<String>();
        ExternalEntityHeader resolvedTarget = resolveHeader(targetFolder);
        MigrationMetadata metadata = bundle.getMetadata();
        if (flatten) {
            // replace all folder dependencies with the (unique) targetFolder
            for (MigrationDependency dep : metadata.getDependencies()) {
                if (dep.getDependency().getType() == EntityType.FOLDER) {
                    metadata.addMappingOrCopy(dep.getDependency(), resolvedTarget, false);
                }
            }
        } else {
            String targetFolderPath = resolvedTarget.getDescription();
            for(ExternalEntityHeader h : metadata.getAllHeaders()) {
                if (EntityType.FOLDER == h.getType() && ! metadata.getRootFolder().equals(h))
                    h.setDescription(targetFolderPath + h.getDescription());
            }
            // don't touch any folders outside the target
            removeFolderMappingsOutsideTarget(metadata.getMappings().entrySet().iterator(), targetFolderPath);
            removeFolderMappingsOutsideTarget(metadata.getCopies().entrySet().iterator(), targetFolderPath);
            // map root folder to target folder
            metadata.addMappingOrCopy(metadata.getRootFolder(), resolvedTarget, false);
        }
        return errors;
    }

    private void removeFolderMappingsOutsideTarget(Iterator<Map.Entry<ExternalEntityHeader,ExternalEntityHeader>> iter, String targetFolderPath) {
        ExternalEntityHeader mappedTarget;
        while (iter.hasNext()) {
            mappedTarget = iter.next().getValue();
            if ( EntityType.FOLDER != mappedTarget.getType() )
                continue;
            if ( !mappedTarget.getDescription().startsWith(targetFolderPath) || mappedTarget.getDescription().equals(targetFolderPath))
                iter.remove();
        }
    }

    private PropertyResolver getResolver(Entity sourceEntity, String propName) throws PropertyResolverException {
        return resolverFactory.getPropertyResolver(MigrationUtils.getTargetType(sourceEntity, propName));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private Collection<String> validateBundle(MigrationBundle bundle, Map<ExternalEntityHeader, Entity> entitiesFromTarget) {
        logger.log(Level.FINEST, "Validating bundle: {0}", bundle);
        Collection<String> errors = new HashSet<String>();
        MigrationMetadata metadata = bundle.getMetadata();

        // check that entity values are available for all headers, either in the bundle or already on the SSG
        for (ExternalEntityHeader header : metadata.getAllHeaders()) {
            if ( ! bundle.hasValueForHeader(header) && ! entitiesFromTarget.containsKey(header) &&
                 ! entitiesFromTarget.containsKey(metadata.getCopiedOrMapped(header)) ) {
                if (header.getType() == EntityType.ENCAPSULATED_ASSERTION) {
                    errors.add("Encapsulated Assertion not found for header: " + header + ". Please import the Encapsulated Assertion on the target prior to migrating.");
                } else {
                    errors.add("Entity not found for header: " + header);
                }
            }
        }

        // mapping requirements
        ExternalEntityHeader dependency;
        for(MigrationDependency dep : metadata.getDependencies()) {
            dependency = dep.getDependency();
            // name-mapping required
            if (dep.getMappingType() == MigrationMappingSelection.REQUIRED && ! metadata.isMapped(dependency))
                errors.add("Unresolved name-mapping: " + dep);

            // value-mapping required
            if (! metadata.isMapped(dependency) && dependency.getValueMapping() == MigrationMappingSelection.REQUIRED && dependency.getMappedValue() == null) {
                errors.add("Unresolved value-mapping: " + dep);
            }
        }

        return errors;
    }

    private PublishedService.WsdlStrategy buildWsdlStrategy( final ExternalEntityHeader serviceHeader,
                                                             final MigrationBundle bundle ) {
        return new ServiceDocumentWsdlStrategy( findServiceDocuments( serviceHeader, bundle.getExportedEntities() ) );
    }

    private Collection<ServiceDocument> findServiceDocuments( final ExternalEntityHeader serviceHeader, final Map<ExternalEntityHeader, Entity> entities  ) {
        Collection<ServiceDocument> serviceDocuments = new ArrayList<ServiceDocument>();

        for (ExternalEntityHeader header : entities.keySet()) {
            if (header.getType() == EntityType.SERVICE_DOCUMENT ) {
                ServiceDocument serviceDocument = (ServiceDocument) entities.get(header);
                if ( Goid.equals(serviceDocument.getServiceId(), serviceHeader.getGoid())  ) {
                    serviceDocuments.add( serviceDocument );
                }
            }
        }

        return serviceDocuments;
    }

    private void findDependenciesRecursive(MigrationMetadata result, ExternalEntityHeader header, Set<ExternalEntityHeader> visited) throws MigrationApi.MigrationException, PropertyResolverException {
        logger.log(Level.FINE, "Finding dependencies for: " + header.toStringVerbose());
        visited.add(header); // marks header as processed
        Entity entity = loadEntity(header);

        for (Method method : entity.getClass().getMethods()) {
            if (MigrationUtils.isDependency(method)) {
                PropertyResolver resolver = getResolver(entity, method.getName());
                Map<ExternalEntityHeader, Set<MigrationDependency>> deps;
                deps = resolver.getDependencies(header, entity, method, MigrationUtils.propertyNameFromGetter(method.getName()));
                for (ExternalEntityHeader depHeader : deps.keySet()) {
                    ExternalEntityHeader resolvedDepHeader = resolveHeader(depHeader);
                    for ( MigrationDependency dependency : deps.get(depHeader) ) {
                        if (dependency != null) { // only the depHeader is useful for inverse dependencies 
                            dependency.setDependency(resolvedDepHeader);
                            result.addDependency(dependency);
                            logger.log(Level.FINE, "Added dependency: " + dependency);
                        }
                        if ( ! visited.contains(resolvedDepHeader) &&
                             (dependency == null || dependency.getMappingType() != MigrationMappingSelection.REQUIRED) &&
                             resolvedDepHeader.getValueMapping() != MigrationMappingSelection.REQUIRED &&
                             ! (depHeader instanceof ValueReferenceEntityHeader) )  {
                            findDependenciesRecursive( result, resolvedDepHeader, visited );
                        }
                    }
                }
            }
        }
    }

    /**
     * "Resolves" an external entity header:
     * - special-case processing for certain types
     * - if the entity can be loaded, the header is reconstructed from the entity, filling in all the fields (e.g. name, version)
     */
    private ExternalEntityHeader resolveHeader(final ExternalEntityHeader header) throws MigrationApi.MigrationException {
        if (header instanceof ValueReferenceEntityHeader)
            return header;
        Entity ent = null;
        try {
            ent = loadEntity(header);
        } catch (MigrationApi.MigrationException e) {
            logger.log(Level.WARNING, "Error resolving header: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        ExternalEntityHeader externalEntityHeader = ent == null ? header : EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(ent));
        if (externalEntityHeader != header)
            externalEntityHeader.addExtraProperties( header.getExtraProperties() );
        enhanceHeader( externalEntityHeader );
        return externalEntityHeader;
    }

    private Set<ExternalEntityHeader> resolveHeaders(final Collection<ExternalEntityHeader> headers) throws MigrationApi.MigrationException {
        Set<ExternalEntityHeader> resolvedHeaders = new LinkedHashSet<ExternalEntityHeader>(headers.size());

        for (ExternalEntityHeader header : headers) {
            resolvedHeaders.add(resolveHeader(header));
        }

        return resolvedHeaders;
    }

    private void enhanceHeader( final ExternalEntityHeader externalEntityHeader ) throws MigrationApi.MigrationException  {
        if ( externalEntityHeader != null && externalEntityHeaderEnhancer != null ) {
            try {
                externalEntityHeaderEnhancer.enhance( externalEntityHeader );
            } catch ( ExternalEntityHeaderEnhancer.EnhancementException e ) {
                throw new MigrationApi.MigrationException( ExceptionUtils.getMessage( e ), e );
            }
        }
    }

    private String composeErrorMessage( final String summary, final ExternalEntityHeader externalHeader, final Exception root ) {
        EntityHeader header = EntityHeaderUtils.fromExternal(externalHeader);
        String message = summary + ":\n";
        if ( header instanceof ServiceHeader) {
            message += header.getType() + ", " + ((ServiceHeader) header).getDisplayName() + getDisplayId(header) + "\ndue to:\n";
        } else {
            message += header.getType() + ", " + (header.getName() == null ? "" : header.getName()) + getDisplayId(header) + "\ndue to:\n";
        }

        String errMsg = ExceptionUtils.getMessage(root);
        String causeMsg = (root == null || root.getCause() == null) ? "" : ExceptionUtils.getMessage(root.getCause());
        message += errMsg;
        if (errMsg == null || ! errMsg.equals(causeMsg)) {
            message += " (" + causeMsg + ")";
        }

        return message;
    }

    private String getDisplayId(EntityHeader header) {
        String id = header instanceof GuidEntityHeader ? ((GuidEntityHeader)header).getGuid() :
                    !header.getGoid().equals(GoidEntity.DEFAULT_GOID) ? Goid.toString(header.getGoid()) : null;
        return id == null ? "" : " (#" + id + ")";
    }

    private Entity loadEntity( final ExternalEntityHeader externalHeader ) throws MigrationApi.MigrationException {
        logger.log(Level.FINEST, "Loading entity for header: {0}", externalHeader);
        EntityHeader header;
        try {
            header = EntityHeaderUtils.fromExternal(externalHeader, false);
        } catch ( IllegalArgumentException e ) {
            throw new MigrationApi.MigrationException("Error processing the header for entity: " + externalHeader.getExternalId(), e);
        }
        Entity ent;
        try {
            // special handling for value-reference entities
            if (header instanceof ValueReferenceEntityHeader) {
                ent = entityCrud.find(EntityHeaderUtils.fromExternal(((ValueReferenceEntityHeader) header).getOwnerHeader()));
            } else {
                ent = entityCrud.find(header); // load the entity
            }
        } catch (Exception e) {
            throw new MigrationApi.MigrationException("Error loading the entity for header: " + header, e);
        }
        if (ent == null)
            throw new MigrationApi.MigrationException("Error loading the entity for header "+ header.getType() +", " + (header.getName()==null? "" : header.getName()) + " (#"+header.getGoid()+")");
        return ent;
    }
}
