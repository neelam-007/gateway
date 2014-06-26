package com.l7tech.server.bundling;

import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.Identity;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.bundling.exceptions.BundleImportException;
import com.l7tech.server.bundling.exceptions.IncorrectMappingInstructionsException;
import com.l7tech.server.bundling.exceptions.TargetExistsException;
import com.l7tech.server.bundling.exceptions.TargetNotFoundException;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Either;
import com.l7tech.util.Eithers;
import com.l7tech.util.Option;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
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
    private PolicyManager policyManager;
    @Inject
    private ServiceManager serviceManager;
    @Inject
    private PolicyVersionManager policyVersionManager;

    /**
     * This will import the given entity bundle. If test is true or there is an error during bundle import nothing is
     * committed and all changes that were made are rolled back
     *
     * @param bundle         The bundle to import
     * @param test           if true the bundle import will be performed but rolled back afterwards and the results of
     *                       the import will be returned. If false the bundle import will be committed it if is
     *                       successful.
     * @param activate       True to activate the updated services and policies.
     * @param versionComment The comment to set for updated/created services and policies
     * @return The mapping results of the bundle import.
     */
    @NotNull
    public List<EntityMappingResult> importBundle(@NotNull final EntityBundle bundle, final boolean test, final boolean activate, @Nullable final String versionComment) {
        if (!test) {
            logger.log(Level.INFO, "Importing bundle!");
        } else {
            logger.log(Level.FINE, "Test Importing bundle!");
        }

        //Perform the bundle import within a transaction so that it can be rolled back if there is an error importing the bundle, or this is a test import.
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        return tt.execute(new TransactionCallback<List<EntityMappingResult>>() {
            @Override
            public List<EntityMappingResult> doInTransaction(final TransactionStatus transactionStatus) {
                //This is the list of mappings to return.
                final List<EntityMappingResult> mappingsRtn = new ArrayList<>(bundle.getMappingInstructions().size());
                //This is a map of entities in the entity bundle to entities that are updated or created on the gateway.
                final Map<EntityHeader, EntityHeader> resourceMapping = new HashMap<>(bundle.getMappingInstructions().size());
                //loop through each mapping instruction to perform the action.
                for (final EntityMappingInstructions mapping : bundle.getMappingInstructions()) {
                    //Get the entity that this mapping is for from the bundle
                    final EntityContainer entity = bundle.getEntity(mapping.getSourceEntityHeader().getStrId(), mapping.getSourceEntityHeader().getType());
                    try {
                        //Find an existing entity to map it to.
                        @Nullable
                        final Entity existingEntity = locateExistingEntity(mapping, entity == null ? null : entity.getEntity(), resourceMapping);
                        @NotNull
                        final EntityMappingResult mappingResult;
                        if (existingEntity != null) {
                            //Use the existing entity
                            if (mapping.shouldFailOnExisting()) {
                                mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new TargetExistsException(mapping, "Fail on existing specified and target exists."));
                                //rollback the transaction
                                transactionStatus.setRollbackOnly();
                            } else {
                                switch (mapping.getMappingAction()) {
                                    case NewOrExisting: {
                                        //Use the existing entity
                                        final EntityHeader targetEntityHeader = EntityHeaderUtils.fromEntity(existingEntity);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.UsedExisting);

                                        // Adds the mapped headers to the resourceMapping map if they are different.
                                        if (!headersMatch(mapping.getSourceEntityHeader(), targetEntityHeader)) {
                                            resourceMapping.put(mapping.getSourceEntityHeader(), targetEntityHeader);
                                        }
                                        break;
                                    }
                                    case NewOrUpdate: {
                                        //update the existing entity
                                        final EntityHeader targetEntityHeader = createOrUpdateResource(entity, Goid.parseGoid(existingEntity.getId()), mapping, resourceMapping, existingEntity, activate, versionComment);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.UpdatedExisting);
                                        break;
                                    }
                                    case AlwaysCreateNew: {
                                        final EntityHeader targetEntityHeader = createOrUpdateResource(entity, null, mapping, resourceMapping, null, activate, versionComment);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.CreatedNew);
                                        break;
                                    }
                                    case Ignore:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader());
                                        break;
                                    default:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new IncorrectMappingInstructionsException(mapping, "Unknown mapping action " + mapping.getMappingAction()));
                                }
                            }
                        } else {
                            if (mapping.shouldFailOnNew()) {
                                mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new TargetNotFoundException(mapping, "Fail on new specified and could not locate existing target."));
                                //rollback the transaction
                                transactionStatus.setRollbackOnly();
                            } else {
                                switch (mapping.getMappingAction()) {
                                    case NewOrExisting:
                                    case NewOrUpdate:
                                    case AlwaysCreateNew: {
                                        //Create a new entity based on the one in the bundle
                                        final EntityHeader targetEntityHeader = createOrUpdateResource(entity, mapping.getSourceEntityHeader().getGoid(), mapping, resourceMapping, null, activate, versionComment);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.CreatedNew);
                                        break;
                                    }
                                    case Ignore:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader());
                                        break;
                                    default:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new IncorrectMappingInstructionsException(mapping, "Unknown mapping action " + mapping.getMappingAction()));
                                }
                            }
                        }
                        mappingsRtn.add(mappingResult);
                    } catch (Exception e) {
                        mappingsRtn.add(new EntityMappingResult(mapping.getSourceEntityHeader(), e));
                        transactionStatus.setRollbackOnly();
                    }
                }

                //need to process generic entities at the end so that cyclical dependencies can be properly replaced
                replaceGenericEntityDependencies(mappingsRtn, resourceMapping);

                //need to process revocation check policies again at the end so that cyclical dependencies can be properly replaced
                replaceRevocationCheckingPolicyDependencies(mappingsRtn, resourceMapping);

                // replace dependencies in policy xml after all entities are created ( can replace circular dependencies)
                replacePolicyDependencies(mappingsRtn, resourceMapping);

                if (test) {
                    transactionStatus.setRollbackOnly();
                }
                return mappingsRtn;
            }
        });
    }

    /**
     * Replace generic entity dependencies
     *
     * @param mappingsRtn     The mappings to find generic entities in to replace dependencies for
     * @param resourceMapping The resource map of dependencies to map to.
     */
    private void replaceGenericEntityDependencies(@NotNull final List<EntityMappingResult> mappingsRtn, @NotNull final Map<EntityHeader, EntityHeader> resourceMapping) {
        for (final EntityMappingResult results : mappingsRtn) {
            //only process the mapping if the action taken was updated or created and the entity is a generic entity.
            if (results.getTargetEntityHeader() != null &&
                    results.isSuccessful() &&
                    (results.getMappingAction().equals(EntityMappingResult.MappingAction.UpdatedExisting) || results.getMappingAction().equals(EntityMappingResult.MappingAction.CreatedNew)) &&
                    (results.getTargetEntityHeader().getType().equals(EntityType.GENERIC))) {
                final TransactionTemplate tt = new TransactionTemplate(transactionManager);
                tt.setReadOnly(false);
                final Exception exception = tt.execute(new TransactionCallback<Exception>() {
                    @Override
                    public Exception doInTransaction(final TransactionStatus transactionStatus) {
                        try {
                            //get the existing generic entity.
                            final Entity existingEntity = entityCrud.find(results.getTargetEntityHeader());

                            if(existingEntity == null) {
                                // this should not happen. The entity should exist
                                transactionStatus.setRollbackOnly();
                                return new FindException("Cannot find updated or created Generic entity with header: " + results.getTargetEntityHeader().toStringVerbose());
                            }

                            //replace the dependencies on the generic entity.
                            dependencyAnalyzer.replaceDependencies(existingEntity, resourceMapping, false);
                            //update the generic entity.
                            entityCrud.update(existingEntity);
                        } catch (Exception e) {
                            transactionStatus.setRollbackOnly();
                            return e;
                        }

                        //flush the newly created object so that it can be found by the entity managers later.
                        transactionStatus.flush();
                        return null;
                    }
                });
                if (exception != null) {
                    //update the mapping to contain the exception.
                    results.makeExceptional(exception);
                }
            }
        }
    }
    /**
     * Replace revocation check policy dependencies
     *
     * @param mappingsRtn     The mappings to find revocation check policies in to replace dependencies for
     * @param resourceMapping The resource map of dependencies to map to.
     */
    private void replaceRevocationCheckingPolicyDependencies(@NotNull final List<EntityMappingResult> mappingsRtn, @NotNull final Map<EntityHeader, EntityHeader> resourceMapping) {
        for (final EntityMappingResult results : mappingsRtn) {
            //only process the mapping if the action taken was updated or created and the entity is a generic entity.
            if (results.getTargetEntityHeader() != null &&
                    results.isSuccessful() &&
                    (results.getMappingAction().equals(EntityMappingResult.MappingAction.UpdatedExisting) || results.getMappingAction().equals(EntityMappingResult.MappingAction.CreatedNew)) &&
                    (results.getTargetEntityHeader().getType().equals(EntityType.REVOCATION_CHECK_POLICY))) {
                final TransactionTemplate tt = new TransactionTemplate(transactionManager);
                tt.setReadOnly(false);
                final Exception exception = tt.execute(new TransactionCallback<Exception>() {
                    @Override
                    public Exception doInTransaction(final TransactionStatus transactionStatus) {
                        try {
                            //get the existing generic entity.
                            final Entity existingEntity = entityCrud.find(results.getTargetEntityHeader());

                            if(existingEntity == null) {
                                // this should not happen. The entity should exist
                                transactionStatus.setRollbackOnly();
                                return new FindException("Cannot find updated or created Revocation Check Policy entity with header: " + results.getTargetEntityHeader().toStringVerbose());
                            }

                            final RevocationCheckPolicy existingPolicy = ((RevocationCheckPolicy)existingEntity);

                            // do replace on cloned entity
                            final RevocationCheckPolicy tempPolicy = existingPolicy.clone();
                            dependencyAnalyzer.replaceDependencies(tempPolicy, resourceMapping, false);

                            //update the generic entity.
                            existingPolicy.setRevocationCheckItems(tempPolicy.getRevocationCheckItems());
                            entityCrud.update(existingPolicy);

                        } catch (Exception e) {
                            transactionStatus.setRollbackOnly();
                            return e;
                        }

                        //flush the newly created object so that it can be found by the entity managers later.
                        transactionStatus.flush();
                        return null;
                    }
                });
                if (exception != null) {
                    //update the mapping to contain the exception.
                    results.makeExceptional(exception);
                }
            }
        }
    }

    private void replacePolicyDependencies(@NotNull final List<EntityMappingResult> mappingsRtn, final Map<EntityHeader, EntityHeader> resourceMapping) {
        // map dependencies for policies and services
        for (final EntityMappingResult results : mappingsRtn) {
            if (results.getTargetEntityHeader() != null &&
                    results.isSuccessful() &&
                    (results.getMappingAction().equals(EntityMappingResult.MappingAction.UpdatedExisting) || results.getMappingAction().equals(EntityMappingResult.MappingAction.CreatedNew)) &&
                    (results.getTargetEntityHeader().getType().equals(EntityType.POLICY) || results.getTargetEntityHeader().getType().equals(EntityType.SERVICE))) {
                final TransactionTemplate tt = new TransactionTemplate(transactionManager);
                tt.setReadOnly(false);
                final Exception exception = tt.execute(new TransactionCallback<Exception>() {
                    @Override
                    public Exception doInTransaction(final TransactionStatus transactionStatus) {
                        try {
                            final Entity existingEntity = entityCrud.find(results.getTargetEntityHeader());
                            if(existingEntity == null) {
                                // this should not happen. The entity should exist
                                transactionStatus.setRollbackOnly();
                                return new FindException("Cannot find updated or created policy or service entity with header: " + results.getTargetEntityHeader().toStringVerbose());
                            }
                            final Policy policy;
                            if (existingEntity instanceof Policy) {
                                policy = (Policy) existingEntity;
                            } else {
                                policy = ((PublishedService) existingEntity).getPolicy();
                            }

                            // do replace entity on the latest policy revision.
                            final String oldPolicyXml = policy.getXml();
                            final PolicyVersion policyVersion = policyVersionManager.findLatestRevisionForPolicy(policy.getGoid());
                            policy.setXml(policyVersion.getXml());

                            //replace on temp cloned policy and use only the replaced xml, other dependencies will have already been replace from the previous processing
                            final Policy tempPolicy = new Policy(policy);
                            dependencyAnalyzer.replaceDependencies(tempPolicy, resourceMapping, true);
                            policy.setXml(tempPolicy.getXml());

                            // save and update policy version's policy xml
                            policyVersion.setXml(policy.getXml());
                            policyVersionManager.update(policyVersion);

                            // save the update policy, or revert back the policy xml for no active version
                            if (policyVersion.isActive()) {
                                if (existingEntity instanceof Policy) {
                                    policyManager.update(policy);
                                } else {
                                    serviceManager.update((PublishedService) existingEntity);
                                }
                            } else {
                                // revert the policy xml for non active version
                                policy.setXml(oldPolicyXml);
                            }


                        } catch (Exception e) {
                            transactionStatus.setRollbackOnly();
                            return e;
                        }

                        //flush the newly created object so that it can be found by the entity managers later.
                        transactionStatus.flush();
                        return null;
                    }
                });
                if (exception != null) {
                    //update the mapping to contain the exception.
                    results.makeExceptional(exception);
                }

            }
        }
    }

    /**
     * Returns true if the headers match. They have id's, names, and guids the same
     *
     * @param header1 The first header
     * @param header2 The second header
     * @return True if the headers match (represent the same entity)
     */
    private boolean headersMatch(@NotNull final EntityHeader header1, @NotNull final EntityHeader header2) {
        //checks if they are GUID headers.
        if (header1 instanceof GuidEntityHeader) {
            if (!(header2 instanceof GuidEntityHeader) || !StringUtils.equals(((GuidEntityHeader) header1).getGuid(), ((GuidEntityHeader) header2).getGuid())) {
                return false;
            }
        }

        //if it is a resource header the uri's must match
        if (header1 instanceof ResourceEntryHeader) {
            if (!(header2 instanceof ResourceEntryHeader) || !StringUtils.equals(((ResourceEntryHeader) header1).getUri(), ((ResourceEntryHeader) header2).getUri())) {
                return false;
            }
        }

        //the id's and names must match
        return header1.equals(header2)
                && StringUtils.equals(header1.getName(), header2.getName());
    }

    /**
     * This will create or update an entity.
     *
     * @param entityContainer The entity to create or update, includes dependent entities. If this is null an exception
     *                        will be thrown
     * @param id              The id of the entity to create or update. If this is null an entity will be created with a
     *                        new id.
     * @param mapping         The mapping instructions for the entity to create or update.
     * @param resourceMapping The existing mappings that have been performed
     * @param existingEntity  The existing entity for update, null for create
     * @param activate        True to activate the updated services and policies. False to leave the current versions
     *                        activated.
     * @param versionComment  The comment to set for updated/created services and policies
     * @return The entity header of the entity that was created or updated.
     * @throws ObjectModelException
     * @throws CannotReplaceDependenciesException
     * @throws ConstraintViolationException
     */
    @NotNull
    private EntityHeader createOrUpdateResource(@Nullable final EntityContainer entityContainer,
                                                @Nullable final Goid id,
                                                @NotNull final EntityMappingInstructions mapping,
                                                @NotNull final Map<EntityHeader, EntityHeader> resourceMapping,
                                                @Nullable final Entity existingEntity,
                                                final boolean activate,
                                                @Nullable final String versionComment) throws ObjectModelException, IncorrectMappingInstructionsException, CannotReplaceDependenciesException {
        if (entityContainer == null) {
            throw new IncorrectMappingInstructionsException(mapping, "Cannot find entity type " + mapping.getSourceEntityHeader().getType() + " with id: " + mapping.getSourceEntityHeader().getGoid() + " in this entity bundle.");
        }

        //validate that the id is the same as the existing entity id if the existing entity is specified.
        if (existingEntity != null && (id == null || !existingEntity.getId().equals(id.toString()))) {
            throw new IllegalStateException("The specified id must match the id of the existing entity if an existing entity is given");
        }

        //create the original entity header
        final EntityHeader originalHeader = EntityHeaderUtils.fromEntity(entityContainer.getEntity());

        //if it is a mapping by name and the mapped name is set it should be preserved here. Or if the mapped GUID is set it should be preserved.
        if (mapping.getTargetMapping() != null && mapping.getTargetMapping().getTargetID() != null) {
            switch (mapping.getTargetMapping().getType()) {
                case NAME:
                    if (entityContainer.getEntity() instanceof NameableEntity) {
                        ((NameableEntity) entityContainer.getEntity()).setName(mapping.getTargetMapping().getTargetID());
                    } else {
                        throw new IncorrectMappingInstructionsException(mapping, "Attempting to map an entity by name that cannot be mapped by name.");
                    }
                    break;
                case GUID:
                    if (entityContainer.getEntity() instanceof GuidEntity) {
                        ((GuidEntity) entityContainer.getEntity()).setGuid(mapping.getTargetMapping().getTargetID());
                    } else {
                        throw new IncorrectMappingInstructionsException(mapping, "Attempting to map an entity by guid that cannot be mapped by guid.");
                    }
                    break;
            }
        }

        //don't replace dependencies on generic entities. This will happen after all other dependencies are replaced in order to allow for circular dependencies
        if (!(entityContainer.getEntity() instanceof GenericEntity)) {
            //not replace dependencies for assertions
            dependencyAnalyzer.replaceDependencies(entityContainer.getEntity(), resourceMapping, false);
        }

        //create/save dependent entities
        beforeCreateOrUpdateEntities(entityContainer, existingEntity, resourceMapping);

        //validate the entity. This should check the entity annotations and see if it contains valid data.
        validate(entityContainer);

        // Create the manage entity within a transaction so that it can be flushed after it is created.
        // Flushing allows it to be found later by the entity managers. It will not be committed to the database until the surrounding parent transaction gets committed
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        final Either<ObjectModelException, Goid> idOrException = tt.execute(new TransactionCallback<Either<ObjectModelException, Goid>>() {
            @Override
            public Either<ObjectModelException, Goid> doInTransaction(final TransactionStatus transactionStatus) {
                @NotNull
                final Goid importedID;
                try {
                    try {
                        //need to process policies and published services specially in order to handle versioning and activation.
                        if (entityContainer.getEntity() instanceof Policy || entityContainer.getEntity() instanceof PublishedService) {
                            //get the policy for versioning.
                            final Policy policy;
                            if (entityContainer.getEntity() instanceof Policy) {
                                policy = (Policy) entityContainer.getEntity();
                            } else {
                                policy = ((PublishedService) entityContainer.getEntity()).getPolicy();
                            }
                            final PersistentEntity policyOrService = (PersistentEntity) entityContainer.getEntity();
                            if (!activate && existingEntity != null) {
                                //if activate is false and we are updating an existing entity. We need to update the policy or service with its existing xml and then create a new policy version with the new xml
                                final String existingXML = (entityContainer.getEntity() instanceof Policy) ? ((Policy) existingEntity).getXml() : ((PublishedService) existingEntity).getPolicy().getXml();
                                policyOrService.setGoid(id);
                                policyOrService.setVersion(((PersistentEntity) existingEntity).getVersion());
                                final String newXML = policy.getXml();
                                policy.setXml(existingXML);
                                //update the policy or service but leave the old xml. Note this should not create a new policy version since the xml will not have changed.
                                entityCrud.update(policyOrService);
                                importedID = policyOrService.getGoid();
                                policy.setXml(newXML);
                                //create a policy checkpoint with the new xml
                                policyVersionManager.checkpointPolicy(policy, false, versionComment, false);
                            } else {
                                if(policyOrService instanceof PublishedService){
                                    if(existingEntity == null){
                                        if(id == null) {
                                            importedID = serviceManager.save((PublishedService) policyOrService);
                                        }else{
                                            serviceManager.save(id,(PublishedService) policyOrService);
                                            importedID = id;
                                        }
                                    }else {
                                        policyOrService.setGoid(id);
                                        policyOrService.setVersion(((PublishedService) existingEntity).getVersion());
                                        serviceManager.update((PublishedService) policyOrService);
                                        importedID = policyOrService.getGoid();
                                    }
                                }else{
                                    if(existingEntity == null){
                                        if(id == null) {
                                            importedID = policyManager.save((Policy) policyOrService);
                                        }else{
                                            policyManager.save(id,(Policy) policyOrService);
                                            importedID = id;
                                        }
                                    }else {
                                        policyOrService.setGoid(id);
                                        policyOrService.setVersion(((Policy) existingEntity).getVersion());
                                        policyManager.update((Policy) policyOrService);
                                        importedID = policyOrService.getGoid();
                                    }
                                }
                                policyVersionManager.checkpointPolicy(policy, true, versionComment, existingEntity == null);
                            }
                        } else {
                            importedID = saveOrUpdateEntity(entityContainer, id, existingEntity);
                        }
                    } catch (ObjectModelException e) {
                        return Either.left(e);
                    }
                    //flush the newly created object so that it can be found by the entity managers later.
                    transactionStatus.flush();
                } catch (Exception e) {
                    //This will catch exceptions like org.springframework.dao.DataIntegrityViolationException or other runtime exceptions
                    return Either.left(new ObjectModelException("Error attempting to save or update " + entityContainer.getEntity().getClass(), e));
                }
                return Either.right(importedID);
            }
        });
        //throw the exception if there was one attempting to save the entity.
        Eithers.extract(idOrException);

        afterCreateOrUpdateEntities(entityContainer, existingEntity == null);

        //create the target entity header
        final EntityHeader targetHeader = EntityHeaderUtils.fromEntity(entityContainer.getEntity());

        //add the header mapping if it has change any ids
        if (!headersMatch(originalHeader, targetHeader)) {
            resourceMapping.put(originalHeader, targetHeader);
        }
        return targetHeader;
    }

    /**
     * This will save or updated a entity
     *
     * @param entityContainer The entity to save of update.
     * @param id              The id of the entity to save or update.
     * @param existingEntity  The existing entity if one exists.
     * @return Return the id that the entity was saved with or the id of the entity that was updated
     * @throws SaveException
     * @throws UpdateException
     */
    @NotNull
    private Goid saveOrUpdateEntity(@NotNull final EntityContainer entityContainer, @Nullable final Goid id, @Nullable final Entity existingEntity) throws SaveException, UpdateException {
        @NotNull
        final Goid importedID;
        if (existingEntity == null) {
            //save a new entity
            if (id == null) {
                importedID = (Goid) entityCrud.save(entityContainer.getEntity());
            } else {
                entityCrud.save(id, entityContainer.getEntity());
                importedID = id;
            }
        } else {
            //existing entity is not null so this will be an update
            if (entityContainer.getEntity() instanceof PersistentEntity) {
                ((PersistentEntity) entityContainer.getEntity()).setGoid(id);
                ((PersistentEntity) entityContainer.getEntity()).setVersion(((PersistentEntity) existingEntity).getVersion());
            }
            entityCrud.update(entityContainer.getEntity());
            //this will not be null if the existing entity is specified
            //noinspection ConstantConditions
            importedID = id;
        }
        return importedID;
    }

    /**
     * This performs any operations needed before the entity in the given entity container is saved or updated. This
     * should be called after the entity in the entity container has had its dependencies mapped.
     *
     * @param entityContainer The entity container to perform operations on before it is updated or saved.
     * @param existingEntity  The existing entity that this entity will map to. May be null if there is no existing
     *                        entity.
     * @param replacementMap  The replacement map is a map of EntityHeaders to replace.
     * @throws ObjectModelException
     */
    private void beforeCreateOrUpdateEntities(@NotNull final EntityContainer entityContainer, @Nullable final Entity existingEntity, @NotNull final Map<EntityHeader, EntityHeader> replacementMap) throws ObjectModelException, CannotReplaceDependenciesException {
        if (entityContainer instanceof JmsContainer) {
            final JmsContainer jmsContainer = ((JmsContainer) entityContainer);
            //need to replace jms connection dependencies
            dependencyAnalyzer.replaceDependencies(jmsContainer.getJmsConnection(), replacementMap, false);
            if (existingEntity == null) {
                //there is no existing jmsConnection so we need to create a new jms connection.
                final Goid connectionId = (Goid) entityCrud.save(jmsContainer.getJmsConnection());
                jmsContainer.getEntity().setConnectionGoid(connectionId);
            } else if (existingEntity instanceof JmsEndpoint) {
                //need to update the existing jms connection
                final JmsEndpoint existingEndpoint = (JmsEndpoint) existingEntity;
                jmsContainer.getJmsConnection().setGoid(existingEndpoint.getConnectionGoid());
                entityCrud.update(jmsContainer.getJmsConnection());
                jmsContainer.getJmsEndpoint().setConnectionGoid(existingEndpoint.getConnectionGoid());
            } else {
                //this should never happen
                throw new IllegalStateException("JMSContainer was mapped to an entity that is not a JmsEndpoint: " + existingEntity.getClass());
            }
        } else if (entityContainer instanceof PublishedServiceContainer) {
            if (existingEntity != null) {
                if (existingEntity instanceof PublishedService) {
                    //need to update the id's and properties of the policy backing the published service so that is can be properly updated.
                    ((PublishedServiceContainer) entityContainer).getPublishedService().getPolicy().setGuid(((PublishedService) existingEntity).getPolicy().getGuid());
                    ((PublishedServiceContainer) entityContainer).getPublishedService().getPolicy().setGoid(((PublishedService) existingEntity).getPolicy().getGoid());
                    ((PublishedServiceContainer) entityContainer).getPublishedService().getPolicy().setVersion(((PublishedService) existingEntity).getPolicy().getVersion());
                } else {
                    //this should never happen
                    throw new IllegalStateException("PublishedServiceContainer was mapped to an entity that is not a PublishedService: " + existingEntity.getClass());
                }
            }
        } else if (entityContainer.getEntity() instanceof EncapsulatedAssertionConfig) {
            //need to find the real policy to attach to the encass so that it can be properly updated. The policy id here should be correct. It will already have been properly mapped.
            final EncapsulatedAssertionConfig encassConfig = ((EncapsulatedAssertionConfig) entityContainer.getEntity());
            final Policy encassPolicy = ((EncapsulatedAssertionConfig) entityContainer.getEntity()).getPolicy();
            if (encassPolicy != null) {
                final Policy policyFound = policyManager.findByPrimaryKey(encassPolicy.getGoid());
                encassConfig.setPolicy(policyFound);
            }
        }
        //if this entity has a folder and it is mapped to an existing entity then ignore the given folderID and use the folderId of the existing entity.
        if(entityContainer.getEntity() instanceof HasFolder && existingEntity != null) {
            if(existingEntity instanceof HasFolder){
                ((HasFolder)entityContainer.getEntity()).setFolder(((HasFolder)existingEntity).getFolder());
            } else {
                //this should never happen
                throw new IllegalStateException("A folderable entity was mapped to an entity that is not folderable: " + existingEntity.getClass());
            }
        }
    }

    /**
     * This performs any necessary action after the entities have been updated.
     *
     * @param entityContainer The entity container that was just updated or created.
     * @param newEntity       true if a new entity was created, false if an existing one was updated.
     * @throws ObjectModelException
     */
    private void afterCreateOrUpdateEntities(@NotNull final EntityContainer entityContainer, final boolean newEntity) throws ObjectModelException {
        if (entityContainer instanceof PublishedServiceContainer) {
            final PublishedServiceContainer publishedServiceContainer = ((PublishedServiceContainer) entityContainer);

            if (!newEntity) {
                //delete and existing service documents for this service.
                final EntityHeaderSet<EntityHeader> serviceDocs = entityCrud.findAll(ServiceDocument.class);
                for (final EntityHeader serviceDocHeader : serviceDocs) {
                    final ServiceDocument serviceDocument = (ServiceDocument) entityCrud.find(serviceDocHeader);
                    if (serviceDocument != null && serviceDocument.getServiceId().equals(((PublishedServiceContainer) entityContainer).getPublishedService().getGoid())) {
                        entityCrud.delete(serviceDocument);
                    }
                }
            }

            //add all service documents for this service.
            for (final ServiceDocument serviceDocument : publishedServiceContainer.getServiceDocuments()) {
                serviceDocument.setGoid(ServiceDocument.DEFAULT_GOID);
                serviceDocument.setServiceId(((PublishedServiceContainer) entityContainer).getPublishedService().getGoid());
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
    private void validate(@NotNull final EntityContainer entityContainer) throws ConstraintViolationException {
        //get any special validation groups for this entity.
        for (final Object entity : entityContainer.getEntities()) {
            final Class[] groupClasses = getValidationGroups(entity);
            //validate the entity
            final Set<ConstraintViolation<Entity>> violations = validator.validate((Entity) entity, groupClasses);
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
     * but sometimes another group is added.
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
     * @param mapping         The entity mapping instructions
     * @param entity          The entity to import
     * @param resourceMapping This is used to get any already mapped entities that could be needed in locating the given
     *                        entity.
     * @return The existing entity that this entity should be mapped to or null if there is no existing entity to map
     * this entity to.
     * @throws BundleImportException This is thrown if the entity mapping instructions are not properly set or there is
     *                               an error locating an identity provider when mapping identities
     */
    @Nullable
    private Entity locateExistingEntity(@NotNull final EntityMappingInstructions mapping, @Nullable final Entity entity, @NotNull final Map<EntityHeader, EntityHeader> resourceMapping) throws BundleImportException {
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
        //get the mapping target identifier. This is either a name, id or guid depending on the mapping instructions.
        final String mappingTarget = getMapTo(mapping);
        //we are not making any changes to the database, just searching for entities.
        tt.setReadOnly(true);
        return Eithers.extract(tt.execute(new TransactionCallback<Either<BundleImportException, Option<Entity>>>() {
            @Override
            public Either<BundleImportException, Option<Entity>> doInTransaction(final TransactionStatus transactionStatus) {
                try {
                    @Nullable
                    final Entity resource;
                    //special processing for identities. This is because we need to find the correct identity provider to find the identity using
                    if ((EntityType.USER.equals(mapping.getSourceEntityHeader().getType()) || EntityType.GROUP.equals(mapping.getSourceEntityHeader().getType()))
                            && entity instanceof Identity) {
                        //The entity is an identity find the identity provider where the existing identity should reside.
                        final Goid idProvider = getMappedIdentityProviderID(((Identity) entity).getProviderId(), resourceMapping);
                        IdentityProvider provider;
                        try {
                            provider = identityProviderFactory.getProvider(idProvider);
                        } catch (FindException e) {
                            provider = null;
                        }
                        if (provider == null) {
                            return Either.<BundleImportException, Option<Entity>>left(new TargetNotFoundException(mapping, "Error looking up identity. Cannot find Identity provider with id: " + idProvider));
                        }

                        //mapping by id if a target mapping is not specified, or if the target mapping type is by id.
                        if (mapping.getTargetMapping() == null || EntityMappingInstructions.TargetMapping.Type.ID.equals(mapping.getTargetMapping().getType())) {
                            //find the target
                            if (EntityType.USER.equals(mapping.getSourceEntityHeader().getType())) {
                                resource = provider.getUserManager().findByPrimaryKey(mappingTarget);
                            } else if (EntityType.GROUP.equals(mapping.getSourceEntityHeader().getType())) {
                                resource = provider.getGroupManager().findByPrimaryKey(mappingTarget);
                            } else {
                                return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Unknown identity type, expected either user or group. Found: " + mapping.getSourceEntityHeader().getType()));
                            }
                        } else if (EntityMappingInstructions.TargetMapping.Type.NAME.equals(mapping.getTargetMapping().getType())) {
                            //mapping by identity name
                            if (EntityType.USER.equals(mapping.getSourceEntityHeader().getType())) {
                                resource = provider.getUserManager().findByLogin(mappingTarget);
                            } else if (EntityType.GROUP.equals(mapping.getSourceEntityHeader().getType())) {
                                resource = provider.getGroupManager().findByName(mappingTarget);
                            } else {
                                return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Unknown identity type, expected either user or group. Found: " + mapping.getSourceEntityHeader().getType()));
                            }
                        } else {
                            return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Unsupported Target Mapping for an Identity: " + mapping.getTargetMapping().getType() + ". Only id and name are supported"));
                        }
                    } else {
                        if (mapping.getTargetMapping() != null) {
                            switch (mapping.getTargetMapping().getType()) {
                                case ID: {
                                    //Find the entity by its id
                                    resource = entityCrud.find(mapping.getSourceEntityHeader().getType().getEntityClass(), mappingTarget);
                                    break;
                                }
                                case NAME: {
                                    //Find the entity by its name
                                    final List<? extends Entity> list = entityCrud.findAll(mapping.getSourceEntityHeader().getType().getEntityClass(), CollectionUtils.MapBuilder.<String, List<Object>>builder().put("name", Arrays.<Object>asList(mappingTarget)).map(), 0, -1, null, null);
                                    if (list.isEmpty()) {
                                        resource = null;
                                    } else if (list.size() > 1) {
                                        return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Found multiple possible target entities found with name: " + mappingTarget));
                                    } else {
                                        resource = list.get(0);
                                    }
                                    break;
                                }
                                case GUID: {
                                    //Find the entity by its guid
                                    final List<? extends Entity> list = entityCrud.findAll(mapping.getSourceEntityHeader().getType().getEntityClass(), CollectionUtils.MapBuilder.<String, List<Object>>builder().put("guid", Arrays.<Object>asList(mappingTarget)).map(), 0, -1, null, null);
                                    if (list.isEmpty()) {
                                        resource = null;
                                    } else if (list.size() > 1) {
                                        return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Found multiple possible target entities found with guid: " + mappingTarget));
                                    } else {
                                        resource = list.get(0);
                                    }
                                    break;
                                }
                                default: {
                                    return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Unsupported Target Mapping: " + mapping.getTargetMapping().getType() + ". Only id, name, and guid are supported"));
                                }
                            }
                        } else {
                            //find the entity by the id in the source header
                            resource = entityCrud.find(mapping.getSourceEntityHeader().getType().getEntityClass(), mappingTarget);
                        }
                    }
                    return Either.rightOption(resource);
                } catch (FindException e) {
                    //return null if a find exception is thrown.
                    return Either.rightOption(null);
                }
            }
        })).toNull();
    }

    /**
     * Gets the mapping target is from the mapping instructions. This is either an id, name or guid
     *
     * @param mapping The mapping instructions to find the target mapping id from.
     * @return The target mapping id
     * @throws IncorrectMappingInstructionsException This is thrown if the mapping instructions are incorrect and a
     *                                               target mapping id cannot be found
     */
    @NotNull
    private String getMapTo(@NotNull final EntityMappingInstructions mapping) throws IncorrectMappingInstructionsException {
        @NotNull
        final String targetMapTo;
        if (mapping.getTargetMapping() != null && mapping.getTargetMapping().getTargetID() != null) {
            //if a target id is specified use it.
            targetMapTo = mapping.getTargetMapping().getTargetID();
        } else {
            //get the mapping type. The default is by ID if none is specified.
            final EntityMappingInstructions.TargetMapping.Type type = mapping.getTargetMapping() != null ? mapping.getTargetMapping().getType() : EntityMappingInstructions.TargetMapping.Type.ID;
            if (EntityMappingInstructions.TargetMapping.Type.ID.equals(type) && mapping.getSourceEntityHeader().getStrId() != null) {
                //mapping by id so find the target id from the source entity id.
                targetMapTo = mapping.getSourceEntityHeader().getStrId();
            } else if (EntityMappingInstructions.TargetMapping.Type.NAME.equals(type) && mapping.getSourceEntityHeader().getName() != null) {
                //mapping by name so find the name from the source header name
                targetMapTo = mapping.getSourceEntityHeader().getName();
            } else if (EntityMappingInstructions.TargetMapping.Type.GUID.equals(type) && mapping.getSourceEntityHeader() instanceof GuidEntityHeader && ((GuidEntityHeader) mapping.getSourceEntityHeader()).getGuid() != null) {
                // mapping by guid so get the target guid from the source.
                targetMapTo = ((GuidEntityHeader) mapping.getSourceEntityHeader()).getGuid();
            } else {
                //cannot find a target id.
                throw new IncorrectMappingInstructionsException(mapping, "Mapping by " + type + " but could not find target " + type + " to map to.");
            }
        }
        return targetMapTo;
    }

    /**
     * Finds an identity provider id that may have been mapped. Otherwise return the given one if one has not been
     * mapped.
     *
     * @param providerGoid    The identity provider id to find
     * @param resourceMapping The mappings to look through
     * @return The identity provider id that has been mapped or the original one if one has not been mapped.
     */
    @NotNull
    private Goid getMappedIdentityProviderID(@NotNull final Goid providerGoid, @NotNull final Map<EntityHeader, EntityHeader> resourceMapping) {
        for (final EntityHeader header : resourceMapping.keySet()) {
            if (header.getGoid().equals(providerGoid) && header.getType().equals(EntityType.ID_PROVIDER_CONFIG))
                return resourceMapping.get(header).getGoid();
        }
        return providerGoid;
    }
}
