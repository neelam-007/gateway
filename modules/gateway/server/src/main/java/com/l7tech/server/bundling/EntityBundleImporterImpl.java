package com.l7tech.server.bundling;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.policy.*;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.audit.AuditsCollector;
import com.l7tech.server.bundling.exceptions.BundleImportException;
import com.l7tech.server.bundling.exceptions.IncorrectMappingInstructionsException;
import com.l7tech.server.bundling.exceptions.TargetExistsException;
import com.l7tech.server.bundling.exceptions.TargetNotFoundException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.processors.DependencyProcessorUtils;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.AliasManager;
import com.l7tech.server.service.ServiceAliasManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.*;
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
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
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
    private RoleManager roleManager;
    @Inject
    private PolicyVersionManager policyVersionManager;
    @Inject
    private AuditContextFactory auditContextFactory;
    @Inject
    private SsgKeyStoreManager keyStoreManager;
    @Inject
    private SsgKeyStoreManager ssgKeyStoreManager;
    @Inject
    private ServiceAliasManager serviceAliasManager;
    @Inject
    private PolicyAliasManager policyAliasManager;
    @Inject
    private ClusterPropertyManager clusterPropertyManager;
    @Inject
    private ClientCertManager clientCertManager;


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
    @Override
    @NotNull
    public List<EntityMappingResult> importBundle(@NotNull final EntityBundle bundle, final boolean test, final boolean activate, @Nullable final String versionComment) {
        if (!test) {
            logger.log(Level.INFO, "Importing bundle!");
        } else {
            logger.log(Level.FINE, "Test Importing bundle!");
        }
        AuditsCollector collector = new AuditsCollector();
        List<EntityMappingResult> results =  AuditContextUtils.doWithAuditsCollector(collector, new Functions.Nullary<List<EntityMappingResult>>() {
            @Override
            public List<EntityMappingResult> call(){
                List<EntityMappingResult> results =  doImportBundle(bundle, test, activate, versionComment);
                return results;
            }
        });

        if(!test) {
            if (containsErrors(results)) {
                logger.log(Level.INFO, "Error importing bundle");
            } else {
                for (Triple<AuditRecord, Object, Collection<AuditDetail>> record : collector) {
                    auditContextFactory.emitAuditRecordWithDetails(record.left, false, record.middle, record.right);
                }
                logger.log(Level.INFO, "Bundle imported.");
            }
        }
        return results;
    }

    private boolean containsErrors(List<EntityMappingResult> results) {
        return Functions.exists(results, new Functions.Unary<Boolean, EntityMappingResult>() {
            @Override
            public Boolean call(EntityMappingResult mapping) {
                //noinspection ThrowableResultOfMethodCallIgnored
                return mapping.getException() != null;
            }
        });
    }

    @NotNull
    private List<EntityMappingResult> doImportBundle(@NotNull final EntityBundle bundle, final boolean test, final boolean activate, @Nullable final String versionComment) {
        //Perform the bundle import within a transaction so that it can be rolled back if there is an error importing the bundle, or this is a test import.
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        return tt.execute(new TransactionCallback<List<EntityMappingResult>>() {
            @Override
            public List<EntityMappingResult> doInTransaction(final TransactionStatus transactionStatus) {
                logger.log(Level.FINEST, "Importing Bundle. # Mappings: " + bundle.getMappingInstructions().size());
                //This is the list of mappings to return.
                final List<EntityMappingResult> mappingsRtn = new ArrayList<>(bundle.getMappingInstructions().size());
                //This is a map of entities in the entity bundle to entities that are updated or created on the gateway.
                final Map<EntityHeader, EntityHeader> resourceMapping = new HashMap<>(bundle.getMappingInstructions().size());
                //loop through each mapping instruction to perform the action.
                final Map<EntityHeader,Callable<String>> cachedPrivateKeyOperations = new HashMap<EntityHeader,Callable<String>>();
                int progressCounter = 1;
                for (final EntityMappingInstructions mapping : bundle.getMappingInstructions()) {
                    logger.log(Level.FINEST, "Processing mapping " + progressCounter++ + " of " + bundle.getMappingInstructions().size() + ". Mapping: " + mapping.getSourceEntityHeader().toStringVerbose());

                    //Get the entity that this mapping is for from the bundle
                    final EntityContainer entity = getEntityContainerFromBundle(mapping, bundle);
                    try {
                        //Find an existing entity to map it to only if we are not ignoring this mapping.
                        @Nullable
                        final Entity existingEntity = EntityMappingInstructions.MappingAction.Ignore.equals(mapping.getMappingAction()) ? null : locateExistingEntity(mapping, entity == null ? null : entity.getEntity(), resourceMapping);
                        @NotNull
                        final EntityMappingResult mappingResult;
                        if (existingEntity != null) {
                            //Use the existing entity
                            if (mapping.shouldFailOnExisting() && !EntityMappingInstructions.MappingAction.Ignore.equals(mapping.getMappingAction())) {
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
                                        final EntityHeader targetEntityHeader = createOrUpdateResource(entity, existingEntity.getId(), mapping, resourceMapping, existingEntity, activate, versionComment, false, cachedPrivateKeyOperations);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.UpdatedExisting);
                                        break;
                                    }
                                    case AlwaysCreateNew: {
                                        //SSG-9047 This is always create new so in the case that there is an existing entity and the GUID's match we need to generate a new GUID for the newly imported entity.
                                        final boolean resetGuid = entity != null && entity.getEntity() instanceof GuidEntity && ((GuidEntity) entity.getEntity()).getGuid() != null && ((GuidEntity) entity.getEntity()).getGuid().equals(((GuidEntity) existingEntity).getGuid());
                                        final EntityHeader targetEntityHeader = createOrUpdateResource(entity, null, mapping, resourceMapping, null, activate, versionComment, resetGuid, cachedPrivateKeyOperations);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.CreatedNew);
                                        break;
                                    }
                                    case Ignore:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), EntityMappingResult.MappingAction.Ignored);
                                        break;
                                    case Delete:
                                        final EntityHeader targetEntityHeader = deleteEntity(existingEntity, cachedPrivateKeyOperations);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.Deleted);
                                        break;
                                    default:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new IncorrectMappingInstructionsException(mapping, "Unknown mapping action " + mapping.getMappingAction()));
                                }
                            }
                        } else {
                            if (mapping.shouldFailOnNew() && !EntityMappingInstructions.MappingAction.Ignore.equals(mapping.getMappingAction())) {
                                mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new TargetNotFoundException(mapping, "Fail on new specified and could not locate existing target"));
                                //rollback the transaction
                                transactionStatus.setRollbackOnly();
                            } else {
                                switch (mapping.getMappingAction()) {
                                    case NewOrExisting:
                                    case NewOrUpdate:
                                    case AlwaysCreateNew: {
                                        //Create a new entity based on the one in the bundle
                                        final EntityHeader targetEntityHeader = createOrUpdateResource(entity,
                                                //use the target id if specified, otherwise use the source id
                                                mapping.getTargetMapping() != null && EntityMappingInstructions.TargetMapping.Type.ID.equals(mapping.getTargetMapping().getType()) && mapping.getTargetMapping().getTargetID() != null ? mapping.getTargetMapping().getTargetID() : mapping.getSourceEntityHeader().getStrId(),
                                                mapping, resourceMapping, null, activate, versionComment, false, cachedPrivateKeyOperations);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.CreatedNew);
                                        break;
                                    }
                                    case Ignore:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), EntityMappingResult.MappingAction.Ignored);
                                        break;
                                    case Delete:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), EntityMappingResult.MappingAction.Ignored);
                                        break;
                                    default:
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new IncorrectMappingInstructionsException(mapping, "Unknown mapping action " + mapping.getMappingAction()));
                                }
                            }
                        }
                        mappingsRtn.add(mappingResult);
                    } catch (Throwable e) {
                        mappingsRtn.add(new EntityMappingResult(mapping.getSourceEntityHeader(), e));
                        transactionStatus.setRollbackOnly();
                    }
                }

                // Do not attempt these post bundle import tasks if the bundle import fail. It will create misleading error messages in the bundle results.
                if (!containsErrors(mappingsRtn)) {
                    //need to process generic entities at the end so that cyclical dependencies can be properly replaced
                    replaceGenericEntityDependencies(mappingsRtn, resourceMapping);

                    //need to process revocation check policies again at the end so that cyclical dependencies can be properly replaced
                    replaceRevocationCheckingPolicyDependencies(mappingsRtn, resourceMapping);

                    // replace dependencies in policy xml after all entities are created ( can replace circular dependencies)
                    replacePolicyDependencies(mappingsRtn, resourceMapping);
                }

                // if no test/errors do private key operations
                if(!test && !containsErrors(mappingsRtn)){
                    for(final EntityHeader header : cachedPrivateKeyOperations.keySet()){
                        try{
                            cachedPrivateKeyOperations.get(header).call();
                        } catch (Throwable e) {
                            // update mapping
                            EntityMappingResult mapping = Functions.grepFirst(mappingsRtn, new Functions.Unary<Boolean, EntityMappingResult>() {
                                @Override
                                public Boolean call(EntityMappingResult entityMappingResult) {
                                    return entityMappingResult.getSourceEntityHeader().equals(header);
                                }
                            });
                            if(mapping != null){
                                mapping.makeExceptional(e);
                            }
                        }
                    }
                }


                if (test || containsErrors(mappingsRtn)) {
                    transactionStatus.setRollbackOnly();
                }
                return mappingsRtn;
            }
        });
    }

    /**
     * Returns an entity container form the entity bundle using the given mapping
     *
     * @param mapping The mapping to find the entity container for
     * @param bundle  The bundle to find the entity container in
     * @return The entity container for this mapping. Or null if there isn't one in the bundle.
     */
    //TODO: This is shared with BundleTransformer need to make it common
    @Nullable
    private EntityContainer getEntityContainerFromBundle(@NotNull final EntityMappingInstructions mapping, @NotNull final EntityBundle bundle) {
        final String id;
        if (EntityType.ASSERTION_ACCESS.equals(mapping.getSourceEntityHeader().getType())) {
            id = mapping.getSourceEntityHeader().getName();
        } else {
            id = mapping.getSourceEntityHeader().getStrId();
        }
        return id == null ? null : bundle.getEntity(id, mapping.getSourceEntityHeader().getType());
    }

    /**
     * This will attempt to delete the given entity from the gateway.
     *
     * @param entity The entity to delete
     * @return The entity header of the deleted entity
     * @throws DeleteException This is thrown if there was an error attempting to delete the entity
     */
    private EntityHeader deleteEntity(@NotNull final Entity entity,
                                      @NotNull final Map<EntityHeader,Callable<String>> cachedPrivateKeyOperations) throws DeleteException {
        // Create the manage entity within a transaction so that it can be flushed after it is created.
        // Flushing allows it to be found later by the entity managers. It will not be committed to the database until the surrounding parent transaction gets committed
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        final Either<DeleteException, EntityHeader> headerOrException = tt.execute(new TransactionCallback<Either<DeleteException, EntityHeader>>() {
            @Override
            public Either<DeleteException, EntityHeader> doInTransaction(final TransactionStatus transactionStatus) {
                if (EntityType.SSG_KEY_ENTRY == EntityType.findTypeByEntity(entity.getClass())) {
                    // check entity exists
                    final int sepIndex = entity.getId().indexOf(":");
                    if (sepIndex < 0) {
                        return Either.left(new DeleteException("Cannot delete private key. Invalid key id: " + entity.getId() + ". Expected id with format: <keystoreId>:<alias>"));
                    }
                    final String keyStoreId = entity.getId().substring(0, sepIndex);
                    final String keyAlias = entity.getId().substring(sepIndex + 1);
                    final SsgKeyFinder keyStore;
                    try {
                        keyStore = keyStoreManager.findByPrimaryKey(GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keyStoreId));
                        if(!keyStore.getType().equals(SsgKeyFinder.SsgKeyStoreType.PKCS12_SOFTWARE))
                            Either.left(new DeleteException("Cannot delete from hardware keystore via migration: " + keyStore.getType() + "."));
                        if(!keyStore.getKeyStore().getAliases().contains(keyAlias)){
                            Either.left(new DeleteException("Cannot find alias from keystore with id: " + keyStoreId + ". Alias: " + keyAlias + "."));
                        }
                    }catch (FindException | KeyStoreException  e) {
                        return Either.left(new DeleteException("Cannot find keystore with id: " + keyStoreId + ". Error message: " + ExceptionUtils.getMessage(e), e));
                    }

                    cachedPrivateKeyOperations.put(EntityHeaderUtils.fromEntity(entity), new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            //need to specially handle deletion of ssg key entries
                            try {
                                keyStore.getKeyStore().deletePrivateKeyEntry(true, null, keyAlias);
                                return null;
                            } catch (KeyStoreException e) {
                                throw new DeleteException("Cannot find delete alias from keystore with id: " + keyStoreId + ". Alias: " + keyAlias + ". Error message: " + ExceptionUtils.getMessage(e), e);
                            }
                        }
                    });

                } else if (entity instanceof Identity) {
                    //need to specially handle deletion of identity entities
                    final Goid providerId = ((Identity) entity).getProviderId();
                    final IdentityProvider identityProvider;
                    try {
                        identityProvider = identityProviderFactory.getProvider(providerId);
                    } catch (FindException e) {
                        return Either.left(new DeleteException("Cannot find identity provider with id: " + providerId + ". Error message: " + ExceptionUtils.getMessage(e), e));
                    }
                    try {
                        if (entity instanceof Group) {
                            identityProvider.getGroupManager().delete((Group) entity);
                        } else if (entity instanceof User) {
                            identityProvider.getUserManager().delete((User) entity);
                        }
                    } catch (DeleteException e) {
                        return Either.left(e);
                    }
                } else if (entity instanceof InterfaceTag) {
                    try {
                        final ClusterProperty interfaceTagsClusterProperty = clusterPropertyManager.findByUniqueName(InterfaceTag.PROPERTY_NAME);

                        final Set<InterfaceTag> interfaceTags = new HashSet<>();
                        if (interfaceTagsClusterProperty != null) {
                            interfaceTags.addAll(InterfaceTag.parseMultiple(interfaceTagsClusterProperty.getValue()));

                            //remove the existing interface tag from the list (returns all but the existing)
                            Set<InterfaceTag> interfaceTagsWithoutExisting = new HashSet<>(Functions.grep(interfaceTags, new Functions.Unary<Boolean, InterfaceTag>() {
                                @Override
                                public Boolean call(InterfaceTag interfaceTagExisting) {
                                    return !(((InterfaceTag) entity).getName()).equals(interfaceTagExisting.getName());
                                }
                            }));
                            //resaves the interface tags with the existing removed.
                            clusterPropertyManager.putProperty(InterfaceTag.PROPERTY_NAME, InterfaceTag.toString(interfaceTagsWithoutExisting));
                        }
                    } catch (ObjectModelException | ParseException e) {
                        return Either.left(new DeleteException("Cannot find interface tag: " + ((InterfaceTag) entity).getName() + ". Error message: " + ExceptionUtils.getMessage(e), e));
                    }
                } else {
                    try {
                        entityCrud.delete(entity);
                    } catch (DeleteException e) {
                        return Either.left(e);
                    }
                }
                return Either.right(EntityHeaderUtils.fromEntity(entity));
            }
        });
        return Eithers.extract(headerOrException);
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
                    (EntityMappingResult.MappingAction.UpdatedExisting.equals(results.getMappingAction()) || EntityMappingResult.MappingAction.CreatedNew.equals(results.getMappingAction())) &&
                    (results.getTargetEntityHeader().getType().equals(EntityType.GENERIC))) {
                final TransactionTemplate tt = new TransactionTemplate(transactionManager);
                tt.setReadOnly(false);
                final Throwable exception = tt.execute(new TransactionCallback<Throwable>() {
                    @Override
                    public Throwable doInTransaction(final TransactionStatus transactionStatus) {
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

                            //flush the newly created object so that it can be found by the entity managers later.
                            transactionStatus.flush();
                            return null;
                        } catch (Throwable e) {
                            transactionStatus.setRollbackOnly();
                            return e;
                        }
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
                    (EntityMappingResult.MappingAction.UpdatedExisting.equals(results.getMappingAction()) || EntityMappingResult.MappingAction.CreatedNew.equals(results.getMappingAction())) &&
                    (results.getTargetEntityHeader().getType().equals(EntityType.REVOCATION_CHECK_POLICY))) {
                final TransactionTemplate tt = new TransactionTemplate(transactionManager);
                tt.setReadOnly(false);
                final Throwable exception = tt.execute(new TransactionCallback<Throwable>() {
                    @Override
                    public Throwable doInTransaction(final TransactionStatus transactionStatus) {
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

                            //flush the newly created object so that it can be found by the entity managers later.
                            transactionStatus.flush();
                            return null;
                        } catch (Throwable e) {
                            transactionStatus.setRollbackOnly();
                            return e;
                        }
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
                    (EntityMappingResult.MappingAction.UpdatedExisting.equals(results.getMappingAction()) || EntityMappingResult.MappingAction.CreatedNew.equals(results.getMappingAction())) &&
                    (results.getTargetEntityHeader().getType().equals(EntityType.POLICY) || results.getTargetEntityHeader().getType().equals(EntityType.SERVICE))) {
                final TransactionTemplate tt = new TransactionTemplate(transactionManager);
                tt.setReadOnly(false);
                final Throwable exception = tt.execute(new TransactionCallback<Throwable>() {
                    @Override
                    public Throwable doInTransaction(final TransactionStatus transactionStatus) {
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

                            //flush the newly created object so that it can be found by the entity managers later.
                            transactionStatus.flush();
                            return null;
                        } catch (Throwable e) {
                            transactionStatus.setRollbackOnly();
                            return e;
                        }
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
                                                @Nullable final String id,
                                                @NotNull final EntityMappingInstructions mapping,
                                                @NotNull final Map<EntityHeader, EntityHeader> resourceMapping,
                                                @Nullable final Entity existingEntity,
                                                final boolean activate,
                                                @Nullable final String versionComment,
                                                final boolean resetGuid,
                                                @NotNull final Map<EntityHeader,Callable<String>> cachedPrivateKeyOperations) throws ObjectModelException, IncorrectMappingInstructionsException, CannotReplaceDependenciesException {
        if (entityContainer == null) {
            throw new IncorrectMappingInstructionsException(mapping, "Cannot find entity type " + mapping.getSourceEntityHeader().getType() + " with id: " + mapping.getSourceEntityHeader().getGoid() + " in this entity bundle.");
        }

        //validate that the id is the same as the existing entity id if the existing entity is specified.
        if (existingEntity != null && (id == null || !existingEntity.getId().equals(id))) {
            throw new IllegalStateException("The specified id must match the id of the existing entity if an existing entity is given");
        }

        //create the original entity header
        final EntityHeader originalHeader = EntityHeaderUtils.fromEntity(entityContainer.getEntity());

        //see SSG-9047
        if(resetGuid) {
            ((GuidEntity)entityContainer.getEntity()).setGuid(UUID.randomUUID().toString());
        }

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
        final Option<ObjectModelException> possibleException = tt.execute(new TransactionCallback<Option<ObjectModelException>>() {
            @Override
            public Option<ObjectModelException> doInTransaction(final TransactionStatus transactionStatus) {
                try {
                    try {
                        //need to process policies and published services specially in order to handle versioning and activation.
                        if (entityContainer.getEntity() instanceof Policy || entityContainer.getEntity() instanceof PublishedService) {
                            @Nullable final Goid goid = id == null ? null : Goid.parseGoid(id);
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
                                policyOrService.setGoid(goid);
                                policyOrService.setVersion(((PersistentEntity) existingEntity).getVersion());
                                final String newXML = policy.getXml();
                                policy.setXml(existingXML);
                                //update the policy or service but leave the old xml. Note this should not create a new policy version since the xml will not have changed.
                                entityCrud.update(policyOrService);
                                policy.setXml(newXML);
                                //create a policy checkpoint with the new xml
                                policyVersionManager.checkpointPolicy(policy, false, versionComment, false);
                            } else {
                                if(policyOrService instanceof PublishedService){
                                    if(existingEntity == null){
                                        if(id == null) {
                                            serviceManager.save((PublishedService) policyOrService);
                                        }else{
                                            serviceManager.save(goid,(PublishedService) policyOrService);
                                        }
                                        //create the roles for the service
                                        serviceManager.createRoles((PublishedService) policyOrService);
                                    }else {
                                        policyOrService.setGoid(goid);
                                        policyOrService.setVersion(((PublishedService) existingEntity).getVersion());
                                        serviceManager.update((PublishedService) policyOrService);
                                    }
                                }else{
                                    if(existingEntity == null){
                                        if(id == null) {
                                            policyManager.save((Policy) policyOrService);
                                        }else{
                                            policyManager.save(goid,(Policy) policyOrService);
                                        }
                                        //create the roles for the policy
                                        policyManager.createRoles((Policy) policyOrService);
                                    }else {
                                        policyOrService.setGoid(goid);
                                        policyOrService.setVersion(((Policy) existingEntity).getVersion());
                                        policyManager.update((Policy) policyOrService);
                                    }
                                }
                                policyVersionManager.checkpointPolicy(policy, true, versionComment, existingEntity == null);
                            }
                        } else if (entityContainer.getEntity() instanceof SsgKeyEntry) {
                            final SsgKeyEntry ssgKeyEntry = (SsgKeyEntry) entityContainer.getEntity();
                            if(existingEntity == null) {
                                final SsgKeyFinder keyFinder = ssgKeyStoreManager.findByPrimaryKey(ssgKeyEntry.getKeystoreId());
                                if(!keyFinder.getType().equals(SsgKeyFinder.SsgKeyStoreType.PKCS12_SOFTWARE)){
                                    throw new ObjectModelException("Cannot update hardware keystore via migration: " + keyFinder.getType());
                                }

                                final SsgKeyStore ssgKeyStore = keyFinder.getKeyStore();
                                cachedPrivateKeyOperations.put(mapping.getSourceEntityHeader(), new Callable<String>() {
                                    @Override
                                    public String call() throws Exception {
                                        final Future<Boolean> futureSuccess = ssgKeyStore.storePrivateKeyEntry(true, null, ssgKeyEntry, false);
                                        if (!futureSuccess.get()) {
                                            throw new ObjectModelException("Error attempting to save a private key: " + ssgKeyEntry.getId());
                                        }
                                        return null;
                                    }
                                });

                            } else {
                                final SsgKeyEntry existingSsgKeyEntry = (SsgKeyEntry) existingEntity;
                                final SsgKeyFinder keyFinder = ssgKeyStoreManager.findByPrimaryKey(existingSsgKeyEntry.getKeystoreId());
                                if(!keyFinder.getType().equals(SsgKeyFinder.SsgKeyStoreType.PKCS12_SOFTWARE)){
                                    throw new ObjectModelException("Cannot update hardware keystore via migration: " + keyFinder.getType());
                                }
                                final SsgKeyStore ssgKeyStore = keyFinder.getKeyStore();

                                //should use the alias of the existing mapped key
                                ssgKeyEntry.setAlias(existingSsgKeyEntry.getAlias());
                                // reset the metadata
                                if(ssgKeyEntry.getKeyMetadata()!=null) {
                                    ssgKeyEntry.getKeyMetadata().setAlias(existingSsgKeyEntry.getAlias());
                                }

                                cachedPrivateKeyOperations.put(mapping.getSourceEntityHeader(), new Callable<String>() {
                                    @Override
                                    public String call() throws Exception  {
                                        final Future<Boolean> futureSuccess = ssgKeyStore.storePrivateKeyEntry(true, null, ssgKeyEntry, true);
                                        if (!futureSuccess.get()) {
                                            throw new ObjectModelException("Error attempting to update a private key: " + ssgKeyEntry.getId());
                                        }
                                        return null;
                                    }
                                });
                            }
                        } else if (entityContainer.getEntity() instanceof Identity) {
                            //need to specially handle deletion of identity entities
                            final Goid providerId = ((Identity) entityContainer.getEntity()).getProviderId();
                            IdentityProvider identityProvider;
                            try {
                                identityProvider = identityProviderFactory.getProvider(providerId);
                            } catch (FindException e) {
                                identityProvider = null;
                            }
                            if(identityProvider == null){
                                throw new ObjectModelException("Error attempting to save or update " + entityContainer.getEntity().getClass() + " with id: " + entityContainer.getEntity().getId() + ". Message: Could not find identity provider with id: " + providerId);
                            }
                            if (entityContainer.getEntity() instanceof GroupBean) {
                                final GroupManager groupManager = identityProvider.getGroupManager();
                                if(existingEntity == null) {
                                    final Group group = groupManager.reify((GroupBean) entityContainer.getEntity());
                                    final String groupId = groupManager.save(id == null ? null : Goid.parseGoid(id), group, null);
                                    ((GroupBean) entityContainer.getEntity()).setUniqueIdentifier(groupId);
                                } else {
                                    final GroupBean groupBean = (GroupBean) entityContainer.getEntity();
                                    groupBean.setUniqueIdentifier(existingEntity.getId());
                                    final Group group = groupManager.reify(groupBean);
                                    if (group instanceof PersistentEntity && existingEntity instanceof PersistentEntity) {
                                        //need to set the version to the existing user version so it can update properly
                                        ((PersistentEntity) group).setVersion(((PersistentEntity) existingEntity).getVersion());
                                    }
                                    groupManager.update(group);
                                }
                            } else if (entityContainer.getEntity() instanceof UserBean) {
                                final UserManager userManager = identityProvider.getUserManager();
                                if(existingEntity == null) {
                                    final User user = userManager.reify((UserBean) entityContainer.getEntity());
                                    final String userId = userManager.save(id == null ? null : Goid.parseGoid(id), user, null);
                                    ((UserBean) entityContainer.getEntity()).setUniqueIdentifier(userId);
                                } else {
                                    final UserBean userBean = (UserBean) entityContainer.getEntity();
                                    userBean.setUniqueIdentifier(existingEntity.getId());

                                    final User user = userManager.reify(userBean);
                                    if(user instanceof InternalUser && existingEntity instanceof InternalUser){
                                        ((InternalUser) user).setPasswordChangesHistory(((InternalUser) existingEntity).getPasswordChangesHistory());
                                    }
                                    if (user instanceof PersistentEntity && existingEntity instanceof PersistentEntity) {
                                        //need to set the version to the existing user version so it can update properly
                                        ((PersistentEntity) user).setVersion(((PersistentEntity) existingEntity).getVersion());
                                    }
                                    userManager.update(user);
                                }
                            } else {
                                throw new ObjectModelException("Error attempting to save or update " + entityContainer.getEntity().getClass() + " with id: " + entityContainer.getEntity().getId() + ". Message: Unexpected Identity type, should be either a UserBean or GroupBean");
                            }
                        } else if (entityContainer.getEntity() instanceof IdentityProviderConfig && ((IdentityProviderConfig)entityContainer.getEntity()).getTypeVal() == IdentityProviderType.INTERNAL.toVal()){
                            // make sure internal identity provider is admin enabled
                            ((IdentityProviderConfig)entityContainer.getEntity()).setAdminEnabled(true);
                            saveOrUpdateEntity(entityContainer.getEntity(), id == null ? null : Goid.parseGoid(id), existingEntity);
                        } else if (entityContainer.getEntity() instanceof InterfaceTag) {
                            final InterfaceTag interfaceTag = (InterfaceTag) entityContainer.getEntity();
                            //get the interfaceTags
                            final ClusterProperty interfaceTagsClusterProperty = clusterPropertyManager.findByUniqueName(InterfaceTag.PROPERTY_NAME);
                            final Set<InterfaceTag> interfaceTags = new HashSet<>();
                            if (interfaceTagsClusterProperty != null) {
                                interfaceTags.addAll(InterfaceTag.parseMultiple(interfaceTagsClusterProperty.getValue()));
                                //find the interface tag to update, or an existing one with the same name
                                InterfaceTag interfaceTagExisting = Functions.grepFirst(interfaceTags, new Functions.Unary<Boolean, InterfaceTag>() {
                                    @Override
                                    public Boolean call(InterfaceTag interfaceTagExisting) {
                                        return (id != null ? id : interfaceTag.getName()).equals(interfaceTagExisting.getName());
                                    }
                                });
                                if (interfaceTagExisting != null) {
                                    if (existingEntity == null) {
                                        //In this case were were expecting to create a new interface tag but this is not possible since one with the same name already exists.
                                        throw new DuplicateObjectException("Attempting to save a new interface tag but one with the same name already exists. Interface tag name: " + interfaceTag.getName());
                                    }
                                    //update the ip patterns
                                    interfaceTagExisting.setIpPatterns(interfaceTag.getIpPatterns());
                                } else {
                                    interfaceTags.add(interfaceTag);
                                }
                            } else {
                                //creates a new interface tag collection (will create a new cluster property)
                                interfaceTags.add(interfaceTag);
                            }
                            clusterPropertyManager.putProperty(InterfaceTag.PROPERTY_NAME, InterfaceTag.toString(interfaceTags));
                        } else {
                            saveOrUpdateEntity(entityContainer.getEntity(), id == null ? null : Goid.parseGoid(id), existingEntity);
                        }
                    } catch (ObjectModelException e) {
                        return Option.some(e);
                    } finally {
                        //flush the newly created object so that it can be found by the entity managers later.
                        transactionStatus.flush();
                    }
                } catch (Exception e) {
                    //This will catch exceptions like org.springframework.dao.DataIntegrityViolationException or other runtime exceptions
                    return Option.some(new ObjectModelException("Error attempting to save or update " + entityContainer.getEntity().getClass() + ". Message: " + ExceptionUtils.getMessage(e), e));
                }
                return Option.none();
            }
        });
        //throw the exception if there was one attempting to save the entity.
        if(possibleException.isSome()){
            throw possibleException.some();
        }

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
     * @param entity The entity to save of update.
     * @param id              The id of the entity to save or update.
     * @param existingEntity  The existing entity if one exists.
     * @return Return the id that the entity was saved with or the id of the entity that was updated
     * @throws SaveException
     * @throws UpdateException
     */
    @NotNull
    private Goid saveOrUpdateEntity(@NotNull final Entity entity, @Nullable final Goid id, @Nullable final Entity existingEntity) throws SaveException, UpdateException {
        @NotNull
        final Goid importedID;
        if (existingEntity == null) {
            //save a new entity
            if (id == null) {
                importedID = (Goid) entityCrud.save(entity);
            } else {
                entityCrud.save(id, entity);
                importedID = id;
            }
        } else {
            //existing entity is not null so this will be an update
            if (entity instanceof PersistentEntity) {
                ((PersistentEntity) entity).setGoid(id);
                ((PersistentEntity) entity).setVersion(((PersistentEntity) existingEntity).getVersion());
            }
            entityCrud.update(entity);
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
                final JmsConnection existingConnection = entityCrud.find(JmsConnection.class, existingEndpoint.getConnectionGoid());
                if(existingConnection != null) {
                    jmsContainer.getJmsConnection().setVersion(existingConnection.getVersion());
                }
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
        } else if (entityContainer.getEntity() instanceof Role && !((Role) entityContainer.getEntity()).isUserCreated()) {
            //if this is not a user created role copy the permissions for the target to updating role
            if(existingEntity != null){
                //set the role permissions to those of the target role
                final Set<Permission> permissions = ((Role)entityContainer.getEntity()).getPermissions();
                permissions.clear();
                for(final Permission permission : ((Role)existingEntity).getPermissions()){
                    permissions.add(permission);
                }

                //if this is the admin role we need to add the admin tag
                if(Role.Tag.ADMIN.equals(((Role) existingEntity).getTag())){
                    ((Role) entityContainer.getEntity()).setTag(Role.Tag.ADMIN);
                }
            } else {
                // TODO somehow reuse logic in RbacRoleResourceFactory
                // this is a new role, must be 'userCreated'
                ((Role) entityContainer.getEntity()).setUserCreated(true);
            }
        } else if (entityContainer.getEntity() instanceof Alias) {
            //need to check that the alias will not be created in the same folder as the policy or service it is aliasing. Or that is it is created in a folder that already has an alias for that policy or service.
            //this checks if it is to be created in a folder with the aliased service or policy
            final EntityHeader serviceOrPolicyHeader = entityCrud.findHeader(entityContainer.getEntity() instanceof PublishedServiceAlias ? EntityType.SERVICE : EntityType.POLICY, ((Alias) entityContainer.getEntity()).getEntityGoid());
            if (serviceOrPolicyHeader instanceof OrganizationHeader) {
                if (Goid.equals(((OrganizationHeader) serviceOrPolicyHeader).getFolderId(), ((Alias) entityContainer.getEntity()).getFolder().getGoid())) {
                    throw new DuplicateObjectException("Cannot create alias in the same folder as the aliased policy or service");
                }
            } else if (serviceOrPolicyHeader == null) {
                final String serviceOrPolicy = entityContainer.getEntity() instanceof PublishedServiceAlias ? "service" : "policy";
                //note this needs to be a ConstraintViolationException and not a FindException so the the proper mapping is returned. If it is a FindException the mapping error type for the alias is TargetNotFound but we actually want InvalidResource
                throw new ConstraintViolationException("Could not find the " + serviceOrPolicy + " for alias. " + serviceOrPolicy + " id: " + ((Alias) entityContainer.getEntity()).getEntityGoid());
            } else {
                throw new IllegalStateException("A policy or service header is expected to be an OrganizationHeader but it was not. Header: " + serviceOrPolicyHeader);
            }

            // This checks if it is to be created in a folder with another alias for the same service or policy
            final AliasManager aliasManager = entityContainer.getEntity() instanceof PublishedServiceAlias ? serviceAliasManager : policyAliasManager;
            final Alias checkAlias = aliasManager.findAliasByEntityAndFolder(((Alias) entityContainer.getEntity()).getEntityGoid(), ((Alias) entityContainer.getEntity()).getFolder().getGoid());
            if (checkAlias != null) {
                throw new DuplicateObjectException("Cannot create alias in the same folder as an alias for the same aliased policy or service");
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
        //if this is a nameable entity and the existing entity exists then preserve the existing entity name
        if(entityContainer.getEntity() instanceof NameableEntity && existingEntity != null ) {
            if(existingEntity instanceof NameableEntity){
                ((NameableEntity) entityContainer.getEntity()).setName(((NameableEntity) existingEntity).getName());
            } else {
                //this should never happen
                throw new IllegalStateException("A NameableEntity entity was mapped to an entity that is not NameableEntity: " + existingEntity.getClass());
            }
        }
        //if this is a GuidEntity and the existing entity exists then preserve the existing entity guid
        if(entityContainer.getEntity() instanceof GuidEntity && existingEntity != null ) {
            if(existingEntity instanceof GuidEntity){
                ((GuidEntity) entityContainer.getEntity()).setGuid(((GuidEntity) existingEntity).getGuid());
            } else {
                //this should never happen
                throw new IllegalStateException("A GuidEntity entity was mapped to an entity that is not GuidEntity: " + existingEntity.getClass());
            }
        }
        //if this is a user and the existing entity exists then preserve the existing entity login
        if(entityContainer.getEntity() instanceof UserBean && existingEntity != null ) {
            if(existingEntity instanceof User){
                ((UserBean) entityContainer.getEntity()).setLogin(((User) existingEntity).getLogin());
                if(existingEntity instanceof InternalUser) {
                    //for internal users the name and login should match
                    ((UserBean) entityContainer.getEntity()).setName(((User) existingEntity).getName());
                }
            } else {
                //this should never happen
                throw new IllegalStateException("A User entity was mapped to an entity that is not User: " + existingEntity.getClass());
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
        } else if (entityContainer instanceof UserContainer && ((UserContainer) entityContainer).getCertificate() != null) {
            //remove the old user cert if any. This needs to be done in a different transaction so that it can be flushed before the new cert is added. If it is not a stale update exception is thrown
            final TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setReadOnly(false);
            final Option<ObjectModelException> possibleException = tt.execute(new TransactionCallback<Option<ObjectModelException>>() {
                @Override
                public Option<ObjectModelException> doInTransaction(final TransactionStatus transactionStatus) {
                    try {
                        //remove the old cert if any
                        try {
                            clientCertManager.revokeUserCert(((UserContainer) entityContainer).getEntity());
                        } catch (ObjectModelException e) {
                            return Option.optional(e);
                        }
                        return Option.none();
                    } finally {
                        transactionStatus.flush();
                    }
                }
            });
            //throw the exception if there was one attempting to remove the old cert.
            if(possibleException.isSome()){
                throw possibleException.some();
            }

            //set the certificate on the user.
            final X509Certificate x509Certificate = ((UserContainer) entityContainer).getCertificate();
            clientCertManager.recordNewUserCert(((UserContainer) entityContainer).getEntity(), x509Certificate , false);
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
                    } else if((EntityType.USER.equals(mapping.getSourceEntityHeader().getType()) || EntityType.GROUP.equals(mapping.getSourceEntityHeader().getType()))
                            && entity == null && !EntityMappingInstructions.MappingAction.Ignore.equals(mapping.getMappingAction())) {
                        return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Given a user or group mapping without the associated user or group item reference"));
                    } else if (EntityType.RBAC_ROLE.equals(mapping.getSourceEntityHeader().getType()) && mapping.getTargetMapping() != null && EntityMappingInstructions.TargetMapping.Type.MAP_BY_ROLE_ENTITY.equals(mapping.getTargetMapping().getType())) {
                        if(entity == null){
                            return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Attempting to map a role by it's entity but the role is not in the bundle"));
                        } else if(((Role) entity).isUserCreated()){
                            return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Attempting to map a role by it's entity but the role is a user created role. Can only map auto created roles this way"));
                        } else if(((Role) entity).getEntityGoid() == null || ((Role) entity).getEntityType() == null){
                            return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Attempting to map a role by it's entity but the role does not specify both the entity type and id"));
                        } else {
                            try {
                                resource = findExistingRole((Role)entity, resourceMapping);
                            } catch (Exception e) {
                                return Either.<BundleImportException, Option<Entity>>left(new TargetNotFoundException(mapping, "Error finding auto generated role to map to: " + ExceptionUtils.getMessage(e)));
                            }
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
                                case MAP_BY_ROLE_ENTITY: {
                                    return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Specified mapping by role entity but the source entity is not appropriate. The source entity should be a role that is not user created and specifies both entity id and entity type" ));
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
     * This will locate an existing role by looking at the given roles entity goid and entity type, finding the mapped entity and finding the same role associated to that mapped entity
     * @param role Thr role to find the existing role for
     * @param resourceMapping The mapped entities
     * @return The existing mapped role. Or null if one cant be found
     * @throws FindException
     */
    @Nullable
    private Entity findExistingRole(@NotNull final Role role, @NotNull final Map<EntityHeader, EntityHeader> resourceMapping) throws FindException {
        if(role.getEntityGoid() != null && role.getEntityType() != null){
            //find if this is a manage or view role
            final String rolePrefix;
            if(role.getName().startsWith(RbacAdmin.ROLE_NAME_PREFIX_READ)){
                rolePrefix = RbacAdmin.ROLE_NAME_PREFIX_READ;
            } else if(role.getName().startsWith(RbacAdmin.ROLE_NAME_PREFIX)){
                rolePrefix = RbacAdmin.ROLE_NAME_PREFIX;
            } else {
                rolePrefix = null;
            }

            if(rolePrefix != null) {
                //Build the header for the roles role.
                final EntityHeader roleEntityHeader = new EntityHeader(role.getEntityGoid(), role.getEntityType(), null, null);
                // find if the role entity has been mapped.
                EntityHeader mappedRoleEntity = DependencyProcessorUtils.findMappedHeader(resourceMapping, roleEntityHeader);
                //if there wasn't a mapped header then use the original header. This could be because the id's didn't change.
                if (mappedRoleEntity == null) {
                    mappedRoleEntity = roleEntityHeader;
                }
                //find all roles for this entity.
                final Collection<Role> roles = roleManager.findEntitySpecificRoles(mappedRoleEntity.getType(), mappedRoleEntity.getGoid());
                if (roles != null && !roles.isEmpty()) {
                    for (Role possibleRole : roles) {
                        if(possibleRole.getName().startsWith(rolePrefix)){
                            return possibleRole;
                        }
                    }
                }
            }
        }
        return null;
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
            } else if (EntityMappingInstructions.TargetMapping.Type.MAP_BY_ROLE_ENTITY.equals(type)) {
                // set the target mapping to the id of the role, this should be the default.
                targetMapTo = mapping.getSourceEntityHeader().getStrId();
            } else {
                //cannot find a target id.
                throw new IncorrectMappingInstructionsException(mapping, "Mapping by " + type + " but could not find target " + type + " to map to");
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
