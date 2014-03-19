package com.l7tech.server.bundling;

import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Either;
import com.l7tech.util.Eithers;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
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

    /**
     * This will import the given entity bundle. If test is true or there is an error during bundle import nothing is
     * committed and all changes that were made are rolled back
     *
     * @param bundle The bundle to import
     * @param test   if true the bundle import will be performed but rolled back afterwards and the results of the
     *               import will be returned. If false the bundle import will be committed it if is successful.
     * @return The mapping results of the bundle import.
     */
    @NotNull
    public List<EntityMappingResult> importBundle(@NotNull final EntityBundle bundle, final boolean test) {
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
                List<EntityMappingResult> mappingsRtn = new ArrayList<>(bundle.getMappingInstructions().size());
                Map<EntityHeader, EntityHeader> resourceMapping = new HashMap<>(bundle.getMappingInstructions().size());
                for (EntityMappingInstructions mapping : bundle.getMappingInstructions()) {
                    //Get the entity that this mapping is for
                    EntityContainer entity = bundle.getEntity(mapping.getSourceEntityHeader().getStrId());
                    final Entity baseEntity = entity == null? null: (Entity)entity.getEntity();
                    //Find an existing entity to map it to.
                    //TODO: move this into the try block?
                    final Entity existingEntity = locateExistingEntity(mapping,baseEntity);
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
                                        EntityHeader targetEntityHeader = createOrUpdateResource(entity, Goid.parseGoid(existingEntity.getId()), mapping, resourceMapping, existingEntity);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.UpdatedExisting);
                                        break;
                                    }
                                    case AlwaysCreateNew: {
                                        EntityHeader targetEntityHeader = createOrUpdateResource(entity, null, mapping, resourceMapping, null);
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
                                        EntityHeader targetEntityHeader = createOrUpdateResource(entity, Goid.parseGoid(entity.getId()), mapping, resourceMapping, null);
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
     * @return The entity header of the entity that was created or updated.
     * @throws ObjectModelException
     * @throws CannotReplaceDependenciesException
     * @throws CannotRetrieveDependenciesException
     * @throws ConstraintViolationException
     */
    @NotNull
    private EntityHeader createOrUpdateResource(@Nullable final EntityContainer entityContainer, @Nullable final Goid id, @NotNull final EntityMappingInstructions mapping, @NotNull final Map<EntityHeader, EntityHeader> resourceMapping, final Entity existingEntity) throws ObjectModelException, CannotReplaceDependenciesException, CannotRetrieveDependenciesException {
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

        //validate the entity. This should check the entity annotations and see if it contains valid data.
        validate((PersistentEntityContainer)entityContainer);

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
        createOrUpdateDependentEntities((PersistentEntityContainer)entityContainer,existingEntity);

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
                        if (existingEntity == null) {
                            if (id == null) {
                                importedID = (Goid) entityCrud.save(baseEntity);
                            } else {
                                entityCrud.save(id, baseEntity);
                                importedID = id;
                            }
                        } else {
                            baseEntity.setGoid(id);
                            entityCrud.update(baseEntity);
                            importedID = id;
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

        //create the target entity header
        EntityHeader targetHeader = EntityHeaderUtils.fromEntity(baseEntity);

        //add the header mapping if it has change any ids
        if (!headersMatch(originalHeader, targetHeader)) {
            resourceMapping.put(originalHeader, targetHeader);
        }
        return targetHeader;
    }

    private void createOrUpdateDependentEntities(PersistentEntityContainer entityContainer, final Entity existingEntity) throws ObjectModelException {
        if(entityContainer instanceof JmsContainer){
            JmsContainer jmsContainer = ((JmsContainer) entityContainer);
            if(existingEntity == null ){
                entityCrud.save(jmsContainer.getJmsConnection().getGoid(),jmsContainer.getJmsConnection());
                jmsContainer.getEntity().setConnectionGoid(jmsContainer.getJmsConnection().getGoid());
            }else{
                if(existingEntity instanceof JmsEndpoint){
                    JmsEndpoint existingEndpoint = (JmsEndpoint)existingEntity;
                    jmsContainer.getJmsConnection().setGoid(existingEndpoint.getConnectionGoid());
                    entityCrud.update(jmsContainer.getJmsConnection());
                    jmsContainer.getJmsEndpoint().setConnectionGoid(existingEndpoint.getConnectionGoid());
                }
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
    private Entity locateExistingEntity(@NotNull final EntityMappingInstructions mapping, final Entity entity) {
        //this needs to be wrapped in a transaction that ignores rollback. We don't need to rollback if a resource cannot be found.
        final TransactionTemplate tt = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        tt.setReadOnly(true);
        try {
            return tt.execute(new TransactionCallback<Entity>() {
                @Override
                public Entity doInTransaction(final TransactionStatus transactionStatus) {
                    try {
                        final Entity resource;
                        //check if should search by name
                        if (mapping.getTargetMapping() != null) {
                            switch (mapping.getTargetMapping().getType()) {
                                case ID: {
                                    //use the source ID if the target ID is null.
                                    final Goid targetID = mapping.getTargetMapping().getTargetID() == null ? mapping.getSourceEntityHeader().getGoid() : Goid.parseGoid(mapping.getTargetMapping().getTargetID());
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
                            resource = entityCrud.find(EntityHeaderUtils.getEntityClass(mapping.getSourceEntityHeader()), mapping.getSourceEntityHeader().getGoid());
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
}
