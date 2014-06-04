package com.l7tech.server.bundling;

import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.Identity;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Either;
import com.l7tech.util.Eithers;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.*;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.groups.Default;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The bundle importer will import an entity bundle to this gateway. Entities in the bundle will be mapped to existing
 * entities on the gateway using the bundle mappings
 */
public class EntityBundleImporterImpl implements EntityBundleImporter {
    private static final Logger logger = Logger.getLogger(EntityBundleImporter.class.getName());

    //This is used to validate the entity before it is updated or inserted into the database.
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Inject
    private PlatformTransactionManager transactionManager;
    @Inject
    private DependencyAnalyzer dependencyAnalyzer;
    @Inject
    private EntityCrud entityCrud;
    @Inject
    private IdentityProviderFactory identityProviderFactory;
    @Inject
    private GenericEntityManager genericEntityManager;
    @Inject
    private PolicyManager policyManager;
    @Inject
    private ServiceManager serviceManager;
    @Inject
    private PolicyVersionManager policyVersionManager;

    /**
     * This will import the given entity bundle. If test is true or there is an error during bundle import nothing is
     * committed and all changes that were made are rolled back
     *
     * @param bundle The bundle to import
     * @param test   if true the bundle import will be performed but rolled back afterwards and the results of the
     *               import will be returned. If false the bundle import will be committed it if is successful.
     * @param activate True to activate the updated services and policies.
     * @param versionComment The comment to set for updated/created services and policies
     * @return The mapping results of the bundle import.
     */
    @NotNull
    public List<EntityMappingResult> importBundle(@NotNull final EntityBundle bundle, final boolean test, final boolean activate, final String versionComment) {
        if (!test) {
            logger.log(Level.INFO, "Importing bundle!");
        } else {
            logger.log(Level.FINE, "Test Importing bundle!");
        }

        //Perform the bundle import within a transaction so that it can be rolled back if there is an error importing the bundle.
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        return tt.execute(new TransactionCallback<List<EntityMappingResult>>() {
            @Override
            public List<EntityMappingResult> doInTransaction(final TransactionStatus transactionStatus) {
                //This is the list of mappings to return.
                final List<EntityMappingResult> mappingsRtn = new ArrayList<>(bundle.getMappingInstructions().size());
                //This is a map of entities in the entity bundle to entities that are updated or created on the gateway.
                final Map<EntityHeader, EntityHeader> resourceMapping = new HashMap<>(bundle.getMappingInstructions().size());
                for (EntityMappingInstructions mapping : bundle.getMappingInstructions()) {
                    //Get the entity that this mapping is for
                    EntityContainer entity = bundle.getEntity(mapping.getSourceEntityHeader().getStrId());
                    final Entity baseEntity = entity == null? null: (Entity)entity.getEntity();
                    //Find an existing entity to map it to.
                    //TODO: move this into the try block?
                    final Entity existingEntity = locateExistingEntity(mapping,baseEntity,resourceMapping);
                    try {
                        final EntityMappingResult mappingResult;
                        if (existingEntity != null) {
                            //Use the existing entity
                            if (mapping.shouldFailOnExisting()) {
                                //TODO: improve this error message
                                mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new IllegalStateException("Entity Already Exists"));
                                transactionStatus.setRollbackOnly();
                            } else {
                                switch (mapping.getMappingAction()) {
                                    case NewOrExisting: {
                                        //Use the existing entity
                                        EntityHeader targetEntityHeader = EntityHeaderUtils.fromEntity(existingEntity);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.UsedExisting);

                                        // Adds the mapped headers to the resourceMapping map if they are different.
                                        if (!headersMatch(mapping.getSourceEntityHeader(), targetEntityHeader)) {
                                            resourceMapping.put(mapping.getSourceEntityHeader(), targetEntityHeader);
                                        }
                                        break;
                                    }
                                    case NewOrUpdate: {
                                        //update the existing entity
                                        EntityHeader targetEntityHeader = createOrUpdateResource(entity, Goid.parseGoid(existingEntity.getId()), mapping, resourceMapping, existingEntity, activate,versionComment);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.UpdatedExisting);
                                        break;
                                    }
                                    case AlwaysCreateNew: {
                                        EntityHeader targetEntityHeader = createOrUpdateResource(entity, null, mapping, resourceMapping, null, activate,versionComment);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.CreatedNew);
                                        break;
                                    }
                                    case Ignore:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader());
                                        break;
                                    default:
                                        throw new IllegalStateException("Unknown mapping action: " + mapping.getMappingAction());
                                }
                            }
                        } else {
                            if (mapping.shouldFailOnNew()) {
                                //TODO: improve this error message
                                mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new FindException("Entity Does Not Exists"));
                                transactionStatus.setRollbackOnly();
                            } else {
                                switch (mapping.getMappingAction()) {
                                    case NewOrExisting:
                                    case NewOrUpdate:
                                    case AlwaysCreateNew: {
                                        //Create a new entity.
                                        EntityHeader targetEntityHeader = createOrUpdateResource(entity, Goid.parseGoid(entity.getId()), mapping, resourceMapping, null, activate,versionComment);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.CreatedNew);
                                        break;
                                    }
                                    case Ignore:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader());
                                        break;
                                    default:
                                        throw new IllegalStateException("Unknown mapping action: " + mapping.getMappingAction());
                                }
                            }
                        }
                        mappingsRtn.add(mappingResult);
                    } catch (Exception e) {
                        mappingsRtn.add(new EntityMappingResult(mapping.getSourceEntityHeader(), e));
                        transactionStatus.setRollbackOnly();
                    }
                }
                //TODO: handle any recursive dependencies (Encapsulated assertions?)
                if (test) {
                    transactionStatus.setRollbackOnly();
                }
                return mappingsRtn;
            }
        });
    }

    /**
     * Returns true if the headers match. They have id's, names, and guids the same
     *
     * @param header1 The first header
     * @param header2 The second header
     * @return True if the headers match (represent the same entity)
     */
    private boolean headersMatch(EntityHeader header1, EntityHeader header2) {
        //checks if they are GUID headers.
        if (header1 instanceof GuidEntityHeader) {
            if (!(header2 instanceof GuidEntityHeader) || !StringUtils.equals(((GuidEntityHeader) header1).getGuid(), ((GuidEntityHeader) header2).getGuid())) {
                return false;
            }
        }

        if (header1 instanceof ResourceEntryHeader) {
            if (!(header2 instanceof ResourceEntryHeader) || !StringUtils.equals(((ResourceEntryHeader) header1).getUri(), ((ResourceEntryHeader) header2).getUri())) {
                return false;
            }
        }

        return header1.equals(header2)
                && StringUtils.equals(header1.getName(), header2.getName());
    }

    /**
     * This will create or update an entity.
     *
     * @param entityContainer The entity to create or update, includes dependent entities
     * @param id              The id of the entity to create or update
     * @param mapping         The mapping instructions for the entity to create or update
     * @param resourceMapping The existing mappings that have been performed
     * @param existingEntity  The existing entity for update, null for create
     * @param active True to activate the updated services and policies.
     * @param versionComment The comment to set for updated/created services and policies
     * @return The entity header of the entity that was created or updated.
     * @throws ObjectModelException
     * @throws CannotReplaceDependenciesException
     * @throws CannotRetrieveDependenciesException
     * @throws ConstraintViolationException
     */
    @NotNull
    private EntityHeader createOrUpdateResource( final EntityContainer entityContainer,
                                                 @Nullable final Goid id,
                                                 @NotNull final EntityMappingInstructions mapping,
                                                 @NotNull final Map<EntityHeader, EntityHeader> resourceMapping,
                                                 final Entity existingEntity,
                                                 final boolean active,
                                                 final String versionComment) throws ObjectModelException, CannotReplaceDependenciesException, CannotRetrieveDependenciesException {
        if (entityContainer == null) {
            throw new IllegalArgumentException("Cannot find entity type " + mapping.getSourceEntityHeader().getType() + " with id: " + mapping.getSourceEntityHeader().getGoid() + " in this entity bundle.");
        }
        if ( !(entityContainer.getEntity() instanceof Entity)){
            throw new IllegalArgumentException("Cannot find entity type " + mapping.getSourceEntityHeader().getType() + " with id: " + mapping.getSourceEntityHeader().getGoid() + " in this entity bundle.");
        }

        //validate that the id is not null if create is false
        if (existingEntity != null && id == null) {
            throw new IllegalArgumentException("Must specify an id when updating an existing entity.");
        }
        if (!(entityContainer instanceof PersistentEntityContainer)) {
            throw new IllegalArgumentException("Cannot update or save a non persisted entity.");
        }

        final PersistentEntity baseEntity = (PersistentEntity)entityContainer.getEntity();

        //create the original entity header
        EntityHeader originalHeader = EntityHeaderUtils.fromEntity(baseEntity);

        //if it is a mapping by name and the mapped name is set it should be preserved here. Or if the mapped GUID is set it should be preserved.
        if (mapping.getTargetMapping() != null && mapping.getTargetMapping().getTargetID() != null) {
            switch (mapping.getTargetMapping().getType()) {
                case NAME:
                    if (baseEntity instanceof NamedEntityImp) {
                        //TODO: consider moving setName into NameEntity interface
                        ((NamedEntityImp) baseEntity).setName(mapping.getTargetMapping().getTargetID());
                    }
                    break;
                case GUID:
                    //TODO: need to add a Guid entity interface
                    //((GuidEntity) entity).setGuid(mapping.getTargetMapping().getTargetID());
                    //break;
                    throw new NotImplementedException("Mapping entities by Guid it not yet fully implemented.");
            }
        }

        //replace dependencies in the entity
        dependencyAnalyzer.replaceDependencies(baseEntity, resourceMapping);

        //create/save dependent entities
        beforeCreateOrUpdateEntities((PersistentEntityContainer) entityContainer, existingEntity, baseEntity);

        //validate the entity. This should check the entity annotations and see if it contains valid data.
        validate((PersistentEntityContainer)entityContainer);

        // Create the managed object within a transaction so that it can be flushed after it is created.
        // Flushing allows it to be found later by the entity managers.
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        Either<ObjectModelException, Goid> idOrException = tt.execute(new TransactionCallback<Either<ObjectModelException, Goid>>() {
            @Override
            public Either<ObjectModelException, Goid> doInTransaction(final TransactionStatus transactionStatus) {
                final Goid importedID;
                try {
                    try {
                        if (baseEntity instanceof GenericEntity){
                            // todo refactor?
                            if (existingEntity == null) {
                                if (id == null) {
                                    importedID = genericEntityManager.save((GenericEntity)baseEntity);
                                } else {
                                    genericEntityManager.save(id, (GenericEntity)baseEntity);
                                    importedID = id;
                                }
                            } else {
                                baseEntity.setGoid(id);
                                baseEntity.setVersion(((PersistentEntity) existingEntity).getVersion());
                                genericEntityManager.update((GenericEntity)baseEntity);
                                importedID = id;
                            }
                        }else if (baseEntity instanceof Policy){
                            boolean newEntity = true;
                            if (existingEntity == null) {
                                if (id == null) {
                                    importedID = policyManager.save((Policy)baseEntity);
                                } else {
                                    policyManager.save(id, (Policy)baseEntity);
                                    importedID = id;
                                }
                                policyManager.createRoles((Policy)baseEntity);
                            } else {
                                baseEntity.setGoid(id);
                                baseEntity.setVersion(((PersistentEntity) existingEntity).getVersion());
                                policyManager.update((Policy) baseEntity);
                                newEntity = false;
                                importedID = id;
                            }
                            policyVersionManager.checkpointPolicy((Policy)baseEntity,active,versionComment,newEntity);
                        }else if (baseEntity instanceof PublishedService){
                            boolean newEntity = true;
                            if (existingEntity == null) {
                                if (id == null) {
                                    importedID = serviceManager.save((PublishedService)baseEntity);
                                } else {
                                    serviceManager.save(id, (PublishedService)baseEntity);
                                    importedID = id;
                                }
                            } else {
                                baseEntity.setGoid(id);
                                baseEntity.setVersion(((PersistentEntity) existingEntity).getVersion());
                                serviceManager.update((PublishedService) baseEntity);
                                newEntity = false;
                                importedID = id;
                            }
                            policyVersionManager.checkpointPolicy(((PublishedService) baseEntity).getPolicy(),active,versionComment,newEntity);
                        }else{
                            if (existingEntity == null) {
                                if (id == null) {
                                    importedID = (Goid) entityCrud.save(baseEntity);
                                } else {
                                    entityCrud.save(id, baseEntity);
                                    importedID = id;
                                }
                            } else {
                                baseEntity.setGoid(id);
                                baseEntity.setVersion(((PersistentEntity) existingEntity).getVersion());
                                entityCrud.update(baseEntity);
                                importedID = id;
                            }
                        }
                    } catch (SaveException | UpdateException e) {
                        return Either.left((ObjectModelException) e);
                    }
                    //flush the newly created object so that it can be found by the entity managers later.
                    transactionStatus.flush();
                } catch (Exception e) {
                    //This will catch exceptions like org.springframework.dao.DataIntegrityViolationException or other runtime exceptions
                    return Either.left(new ObjectModelException("Error attempting to save or update " + baseEntity.getClass(), e));
                }
                return Either.right(importedID);
            }
        });
        //throw the exception if there was one attempting to save the entity.
        Eithers.extract(idOrException);

        afterCreateOrUpdateEntities(entityContainer,baseEntity, existingEntity);

        //create the target entity header
        EntityHeader targetHeader = EntityHeaderUtils.fromEntity(baseEntity);

        //add the header mapping if it has change any ids
        if (!headersMatch(originalHeader, targetHeader)) {
            resourceMapping.put(originalHeader, targetHeader);
        }
        return targetHeader;
    }

    private void beforeCreateOrUpdateEntities(PersistentEntityContainer entityContainer, final Entity existingEntity, final PersistentEntity baseEntity) throws ObjectModelException {
        if(entityContainer instanceof JmsContainer){
            JmsContainer jmsContainer = ((JmsContainer) entityContainer);
            if(existingEntity == null ){
                Goid connectionId = (Goid)entityCrud.save(jmsContainer.getJmsConnection());
                jmsContainer.getEntity().setConnectionGoid(connectionId);
            }else{
                if(existingEntity instanceof JmsEndpoint){
                    JmsEndpoint existingEndpoint = (JmsEndpoint)existingEntity;
                    jmsContainer.getJmsConnection().setGoid(existingEndpoint.getConnectionGoid());
                    entityCrud.update(jmsContainer.getJmsConnection());
                    jmsContainer.getJmsEndpoint().setConnectionGoid(existingEndpoint.getConnectionGoid());
                }
            }
        }else if(entityContainer instanceof PublishedServiceContainer){
            if(existingEntity!=null){
                ((PublishedService)baseEntity).getPolicy().setGuid(((PublishedService)existingEntity).getPolicy().getGuid());
                ((PublishedService)baseEntity).getPolicy().setGoid(((PublishedService)existingEntity).getPolicy().getGoid());
                ((PublishedService)baseEntity).getPolicy().setVersion(((PublishedService)existingEntity).getPolicy().getVersion());
            }
        }else if(baseEntity instanceof EncapsulatedAssertionConfig){
            EncapsulatedAssertionConfig encassConfig = ((EncapsulatedAssertionConfig)baseEntity);
            Goid policyGoid = ((EncapsulatedAssertionConfig) baseEntity).getPolicy().getGoid();
            Entity policyFound = entityCrud.find(new EntityHeader(policyGoid, EntityType.POLICY,null,null));
            encassConfig.setPolicy((Policy)policyFound);
        }
    }

    private void afterCreateOrUpdateEntities(EntityContainer entityContainer, final PersistentEntity baseEntity, final Entity existingEntity) throws ObjectModelException {
        if(entityContainer instanceof PublishedServiceContainer){
            PublishedServiceContainer publishedServiceContainer = ((PublishedServiceContainer) entityContainer);

            if(existingEntity != null){
                EntityHeaderSet<EntityHeader> serviceDocs = entityCrud.findAll(ServiceDocument.class);
                for ( final EntityHeader serviceDocHeader : serviceDocs ) {
                    ServiceDocument serviceDocument = (ServiceDocument)entityCrud.find(serviceDocHeader);
                    if(serviceDocument != null && serviceDocument.getServiceId().equals(baseEntity.getGoid())){
                        entityCrud.delete(serviceDocument);
                    }
                }
            }

            for ( final ServiceDocument serviceDocument : publishedServiceContainer.getServiceDocuments() ) {
                serviceDocument.setGoid( ServiceDocument.DEFAULT_GOID );
                serviceDocument.setServiceId(baseEntity.getGoid());
                entityCrud.save(serviceDocument);
            }
        }
    }

    /**
     * This will validate the entity using annotations that if has declared on it fields and methods.
     *
     * @param entityContainer The entity to validate
     * @throws ConstraintViolationException This is thrown if the entity is invalid.
     */
    private void validate(@NotNull PersistentEntityContainer entityContainer) throws ConstraintViolationException {
        //get any special validation groups for this entity.


        for(Object entity: entityContainer.getEntities()){
            final Class[] groupClasses = getValidationGroups(entity);
            //validate the entity
            final Set<ConstraintViolation<Entity>> violations = validator.validate((Entity)entity, groupClasses);
            if (!violations.isEmpty()) {
                //the entity is invalid. Create a nice exception message.
                final StringBuilder validationReport = new StringBuilder("Invalid Value: ");
                boolean first = true;
                for (final ConstraintViolation<Entity> violation : violations) {
                    if (!first) validationReport.append('\n');
                    first = false;
                    validationReport.append(violation.getPropertyPath().toString());
                    validationReport.append(' ');
                    validationReport.append(violation.getMessage());
                }
                throw new ConstraintViolationException(validationReport.toString());
            }
        }
    }

    /**
     * This will return the validation groups for an entity Usually the {@link javax.validation.groups.Default} group
     * but sometimes another froup is added.
     *
     * @param entity The entity to get the validation groups for
     * @return The array of validation groups.
     */
    @NotNull
    private Class[] getValidationGroups(@NotNull final Object entity) {
        if (entity instanceof JmsEndpoint && !((JmsEndpoint) entity).isTemplate()) {
            return new Class[]{Default.class, JmsEndpoint.StandardValidationGroup.class};
        } else if (entity instanceof JmsConnection && !((JmsConnection) entity).isTemplate()) {
            return new Class[]{Default.class, JmsConnection.StandardValidationGroup.class};
        } else if (entity instanceof PublishedService && ((PublishedService) entity).isSoap()) {
            return new Class[]{Default.class, PublishedService.SoapValidationGroup.class};
        } else if (entity instanceof Policy && ((Policy) entity).getType() == PolicyType.GLOBAL_FRAGMENT) {
            return new Class[]{Default.class, Policy.GlobalPolicyValidationGroup.class};
        } else {
            return new Class[]{Default.class};
        }
    }

    /**
     * Locates an existing entity that this entity should be mapped to given the entity mapping instructions and the
     * entity to import.
     *
     * @param mapping The entity mapping instructions
     * @param entity  The entity to import
     * @return The existing entity that this entity should be mapped to or null if there is no existing entity to mapp
     * this entity to.
     */
    @Nullable
    private Entity locateExistingEntity(@NotNull final EntityMappingInstructions mapping, final Entity entity,final Map<EntityHeader, EntityHeader> resourceMapping ) {
        //this needs to be wrapped in a transaction that ignores rollback. We don't need to rollback if a resource cannot be found.
        //Wrap the transaction manager so that we can ignore rollback.
        final TransactionTemplate tt = new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return transactionManager.getTransaction(definition);
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {
                transactionManager.commit(status);
            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {
                //do nothing here. We don't want to rollback when an existing entity cannot be found.
            }
        });
        tt.setReadOnly(true);
        try {
            return tt.execute(new TransactionCallback<Entity>() {
                @Override
                public Entity doInTransaction(final TransactionStatus transactionStatus) {

                    try {
                        final Entity resource;
                        //check if should search by name

                        // todo refactor -  user/group, generic entity, general
                        if ((EntityType.USER.equals(mapping.getSourceEntityHeader().getType()) || EntityType.GROUP.equals(mapping.getSourceEntityHeader().getType()))
                                && entity instanceof Identity) {
                            final Goid idProvider = getMappedIdentityProviderID(((Identity)entity).getProviderId(),resourceMapping);
                            IdentityProvider provider = identityProviderFactory.getProvider(idProvider);
                            if (provider == null) return null;

                            if (mapping.getTargetMapping() != null){
                                switch (mapping.getTargetMapping().getType()) {
                                    case ID:{
                                        final String targetID = mapping.getTargetMapping().getTargetID() == null ? mapping.getSourceEntityHeader().getStrId() : mapping.getTargetMapping().getTargetID();
                                        if (mapping.getSourceEntityHeader().getType() == EntityType.USER) {
                                            return provider.getUserManager().findByPrimaryKey(targetID);
                                        } else if (mapping.getSourceEntityHeader().getType() == EntityType.GROUP) {
                                            return provider.getGroupManager().findByPrimaryKey(targetID);
                                        }
                                    }
                                    break;
                                    case NAME:{
                                        String mapTo = mapping.getTargetMapping().getTargetID();
                                        if (mapTo == null) return null;
                                        if (mapping.getSourceEntityHeader().getType() == EntityType.USER) {
                                            return provider.getUserManager().findByLogin(mapTo);
                                        } else if (mapping.getSourceEntityHeader().getType() == EntityType.GROUP) {
                                            return provider.getGroupManager().findByName(mapTo);
                                        }
                                    }
                                    default:
                                        return null;
                                }
                            }
                            //find the entity by the id in the source header
                            if (mapping.getSourceEntityHeader().getType() == EntityType.USER) {
                                return provider.getUserManager().findByPrimaryKey(mapping.getSourceEntityHeader().getStrId());
                            } else if (mapping.getSourceEntityHeader().getType() == EntityType.GROUP) {
                                return provider.getGroupManager().findByPrimaryKey(mapping.getSourceEntityHeader().getStrId());
                            }
                        }
                        if(mapping.getSourceEntityHeader().getType().equals(EntityType.GENERIC)){
                            if (mapping.getTargetMapping() != null){
                                switch (mapping.getTargetMapping().getType()) {
                                    case ID:{
                                        final String targetID = mapping.getTargetMapping().getTargetID() == null ? mapping.getSourceEntityHeader().getStrId() : mapping.getTargetMapping().getTargetID();
                                        return genericEntityManager.findByPrimaryKey(Goid.parseGoid(targetID));
                                    }
                                    case NAME:{
                                        String mapTo = mapping.getTargetMapping().getTargetID();
                                        if (mapTo == null) return null;
                                        return genericEntityManager.findByUniqueName(mapTo);
                                    }
                                    default:
                                        return null;
                                }
                            }
                            //find the entity by the id in the source header
                            return genericEntityManager.findByPrimaryKey(mapping.getSourceEntityHeader().getGoid());
                        }

                        if (mapping.getTargetMapping() != null) {

                            switch (mapping.getTargetMapping().getType()) {
                                case ID: {
                                    //use the source ID if the target ID is null.
                                    final String targetID = mapping.getTargetMapping().getTargetID() == null ? mapping.getSourceEntityHeader().getStrId() : mapping.getTargetMapping().getTargetID();
                                    //Find the entity by its id
                                    resource = entityCrud.find(EntityHeaderUtils.getEntityClass(mapping.getSourceEntityHeader()), targetID);
                                    break;
                                }
                                case NAME: {
                                    String mapTo = mapping.getTargetMapping().getTargetID();
                                    if (mapTo == null) {
                                        //If the name property is not set get the name form the entity in the bundle
                                        mapTo = ((NamedEntity) entity).getName();
                                    }
                                    //Find the entity by its name
                                    List<? extends Entity> list = entityCrud.findAll(EntityHeaderUtils.getEntityClass(mapping.getSourceEntityHeader()), CollectionUtils.MapBuilder.<String, List<Object>>builder().put("name", Arrays.<Object>asList(mapTo)).map(), 0, 1, null, null);
                                    if (list.isEmpty()) {
                                        return null;
                                    }
                                    resource = list.get(0);
                                    break;
                                }
                                case GUID: {
                                    //Find the entity by its guid
                                    List<? extends Entity> list = entityCrud.findAll(EntityHeaderUtils.getEntityClass(mapping.getSourceEntityHeader()), CollectionUtils.MapBuilder.<String, List<Object>>builder().put("guid", Arrays.<Object>asList(mapping.getTargetMapping().getTargetID())).map(), 0, 1, null, null);
                                    if (list.isEmpty()) {
                                        return null;
                                    }
                                    resource = list.get(0);
                                    break;
                                }
                                default: {
                                    throw new IllegalArgumentException("Unknown target type: " + mapping.getTargetMapping().getType());
                                }
                            }
                        } else {
                            //find the entity by the id in the source header
                            resource = entityCrud.find(EntityHeaderUtils.getEntityClass(mapping.getSourceEntityHeader()), mapping.getSourceEntityHeader().getStrId());
                        }
                        return resource;
                    } catch (FindException e) {
                        return null;
                    }
                }
            });
        } catch (UnexpectedRollbackException e) {
            //ignore this exception. It will be thrown if an entity cannot be found.
            return null;
        }
    }

    private Goid getMappedIdentityProviderID(Goid providerGoid, Map<EntityHeader, EntityHeader> resourceMapping) {
        for(EntityHeader header: resourceMapping.keySet()){
            if(header.getGoid().equals(providerGoid) && header.getType().equals(EntityType.ID_PROVIDER_CONFIG) )
                return resourceMapping.get(header).getGoid();
        }
        return providerGoid;
    }
}
