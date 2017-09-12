package com.l7tech.server.bundling;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.objectmodel.polback.PolicyBackedServiceOperation;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.audit.AuditsCollector;
import com.l7tech.server.bundling.exceptions.*;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.module.ServerModuleFileManager;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.processors.DependencyProcessorUtils;
import com.l7tech.server.security.PasswordEnforcerManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.ProtectedEntityTracker;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.AliasManager;
import com.l7tech.server.service.ServiceAliasManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
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
    private FolderManager folderManager;
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
    @Inject
    private PolicyCache policyCache;
    @Inject
    private ServerModuleFileManager serverModuleFileManager;
    @Inject
    private ProtectedEntityTracker protectedEntityTracker;

    @Inject
    private PasswordEnforcerManager passwordEnforcerManager;

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
                //keeps track of policies deleted so far
                final Set<Goid> deletedPolicyIds = new HashSet<>();
                int progressCounter = 1;
                for (final EntityMappingInstructions mapping : bundle.getMappingInstructions()) {
                    logger.log(Level.FINEST, "Processing mapping " + progressCounter++ + " of " + bundle.getMappingInstructions().size() + ". Mapping: " + mapping.getSourceEntityHeader().toStringVerbose());

                    //Get the entity that this mapping is for from the bundle
                    final EntityContainer entity = getEntityContainerFromBundle(mapping, bundle);
                    try {
                        //Find an existing entity to map it to only if we are not ignoring this mapping.
                        @Nullable
                        final Entity existingEntity = EntityMappingInstructions.MappingAction.Ignore.equals(mapping.getMappingAction()) ? null : locateExistingEntity(mapping, entity == null ? null : entity.getEntity(), bundle, resourceMapping);
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
                                        // check if entity is read-only
                                        if (isReadOnly(existingEntity)) {
                                            // don't update a read-only entity
                                            if (logger.isLoggable(Level.FINE)) {
                                                logger.log(
                                                        Level.FINE,
                                                        "Denying NewOrUpdate to Read-Only Entity: " + existingEntity.getId() + " of Type: " + EntityType.findTypeByEntity(existingEntity.getClass()).name()
                                                );
                                            }
                                            mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new TargetReadOnlyException(mapping, "Not possible to update"));
                                            transactionStatus.setRollbackOnly();
                                        } else {
                                            //update the existing entity
                                            final EntityHeader targetEntityHeader = createOrUpdateResource(entity, existingEntity.getId(), mapping, resourceMapping, existingEntity, activate, versionComment, false, cachedPrivateKeyOperations);
                                            mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.UpdatedExisting);
                                        }
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
                                        // check if entity is read-only
                                        if (isReadOnly(existingEntity)) {
                                            // don't update a read-only entity
                                            if (logger.isLoggable(Level.FINE)) {
                                                logger.log(
                                                        Level.FINE,
                                                        "Denying Delete to Read-Only Entity: " + existingEntity.getId() + " of Type: " + EntityType.findTypeByEntity(existingEntity.getClass()).name()
                                                );
                                            }
                                            mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), new TargetReadOnlyException(mapping, "Not possible to delete"));
                                            transactionStatus.setRollbackOnly();
                                        } else {
                                            final EntityHeader targetEntityHeader = deleteEntity(existingEntity, mapping, cachedPrivateKeyOperations, deletedPolicyIds);
                                            mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.Deleted);
                                        }
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
                                    case NewOrUpdate: {
                                        //Create a new entity based on the one in the bundle
                                        final EntityHeader targetEntityHeader = createOrUpdateResource(entity,
                                                //use the target id if specified, otherwise use the source id
                                                mapping.getTargetMapping() != null && EntityMappingInstructions.TargetMapping.Type.ID.equals(mapping.getTargetMapping().getType()) && mapping.getTargetMapping().getTargetID() != null ? mapping.getTargetMapping().getTargetID() : mapping.getSourceEntityHeader().getStrId(),
                                                mapping, resourceMapping, null, activate, versionComment, false, cachedPrivateKeyOperations);
                                        mappingResult = new EntityMappingResult(mapping.getSourceEntityHeader(), targetEntityHeader, EntityMappingResult.MappingAction.CreatedNew);
                                        break;
                                    }
                                    case AlwaysCreateNew: {
                                        //use the target id if specified, otherwise use the source id
                                        String id = mapping.getTargetMapping() != null && EntityMappingInstructions.TargetMapping.Type.ID.equals(mapping.getTargetMapping().getType()) && mapping.getTargetMapping().getTargetID() != null ? mapping.getTargetMapping().getTargetID() : mapping.getSourceEntityHeader().getStrId();

                                        //Create a new entity based on the one in the bundle with a different id if one with same id exists
                                        if(entityCrud.find(new EntityHeader(id, (EntityType)entity.getId().right, null, null)) != null){
                                            id = null;
                                        }

                                        final EntityHeader targetEntityHeader = createOrUpdateResource(entity,
                                                id,
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
                    final TransactionTemplate tt = new TransactionTemplate(transactionManager);
                    tt.setReadOnly(false);
                    tt.execute(new TransactionCallback<Void>() {
                        @Override
                        public Void doInTransaction(final TransactionStatus transactionStatus) {
                                for(final EntityHeader header : cachedPrivateKeyOperations.keySet()){
                                    // update mapping
                                    EntityMappingResult mapping = Functions.grepFirst(mappingsRtn, new Functions.Unary<Boolean, EntityMappingResult>() {
                                        @Override
                                        public Boolean call(EntityMappingResult entityMappingResult) {
                                            return entityMappingResult.getSourceEntityHeader().equals(header);
                                        }
                                    });
                                    if(mapping == null) {
                                        //this shouldn't really happen.
                                        throw new IllegalStateException("Could not find mapping result for " + header.toStringVerbose());
                                    }
                                    try {
                                        try{
                                            cachedPrivateKeyOperations.get(header).call();
                                        } finally {
                                            //flush the transaction so that any error get thrown now.
                                            transactionStatus.flush();
                                        }
                                    } catch (Throwable e) {
                                        mapping.makeExceptional(e);
                                        transactionStatus.setRollbackOnly();
                                    }
                                }
                            return null;
                        }
                    });
                }


                if (test || containsErrors(mappingsRtn)) {
                    transactionStatus.setRollbackOnly();
                }
                return mappingsRtn;
            }
        });
    }

    /**
     * Determine if the specified {@code entity} is read-only.
     *
     * @param entity    entity to check.  Required and cannot be {@code null}.
     * @return {@code true} if {@link #protectedEntityTracker} is not {@code null}, entity protection is enabled and
     * the specified {@code entity} is marked as read-only.
     */
    private boolean isReadOnly(@NotNull final Entity entity) {
        return protectedEntityTracker != null &&
                protectedEntityTracker.isEntityProtectionEnabled() &&
                protectedEntityTracker.isReadOnlyEntity(entity);
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
     * @param deletedPolicyIds policies deleted so far.
     * @return The entity header of the deleted entity
     * @throws DeleteException This is thrown if there was an error attempting to delete the entity
     */
    private EntityHeader deleteEntity(@NotNull final Entity entity, @NotNull final EntityMappingInstructions mapping,
                                      @NotNull final Map<EntityHeader, Callable<String>> cachedPrivateKeyOperations, @NotNull final Set<Goid> deletedPolicyIds) throws IncorrectMappingInstructionsException, DeleteException {
        // Create the manage entity within a transaction so that it can be flushed after it is created.
        // Flushing allows it to be found later by the entity managers. It will not be committed to the database until the surrounding parent transaction gets committed
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        final Either<Either<DeleteException, IncorrectMappingInstructionsException>, EntityHeader> headerOrException = tt.execute(new TransactionCallback<Either<Either<DeleteException, IncorrectMappingInstructionsException>, EntityHeader>>() {
            @Override
            public Either<Either<DeleteException, IncorrectMappingInstructionsException>, EntityHeader> doInTransaction(final TransactionStatus transactionStatus) {
                try {
                    try {
                        if (EntityType.SSG_KEY_ENTRY == EntityType.findTypeByEntity(entity.getClass())) {
                            // check entity exists
                            final int sepIndex = entity.getId().indexOf(":");
                            if (sepIndex < 0) {
                                return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>left(new DeleteException("Cannot delete private key. Invalid key id: " + entity.getId() + ". Expected id with format: <keystoreId>:<alias>")));
                            }
                            final String keyStoreId = entity.getId().substring(0, sepIndex);
                            final String keyAlias = entity.getId().substring(sepIndex + 1);
                            final SsgKeyFinder keyStore;
                            try {
                                keyStore = keyStoreManager.findByPrimaryKey(GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keyStoreId));
                                if (!keyStore.getType().equals(SsgKeyFinder.SsgKeyStoreType.PKCS12_SOFTWARE))
                                    return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>left(new DeleteException("Cannot delete from hardware keystore via migration: " + keyStore.getType())));
                            } catch (FindException | KeyStoreException e) {
                                return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>left(new DeleteException("Cannot find keystore with id: " + keyStoreId, e)));
                            }

                            cachedPrivateKeyOperations.put(mapping.getSourceEntityHeader(), new Callable<String>() {
                                @Override
                                public String call() throws Exception {
                                    //need to specially handle deletion of ssg key entries
                                    try {
                                        keyStore.getKeyStore().deletePrivateKeyEntry(true, null, keyAlias);
                                        return null;
                                    } catch (KeyStoreException e) {
                                        throw new DeleteException("Cannot find delete alias from keystore with id: " + keyStoreId + ". Alias: " + keyAlias, e);
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
                                return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>left(new DeleteException("Cannot find identity provider with id: " + providerId, e)));
                            }
                            try {
                                if (entity instanceof Group) {
                                    identityProvider.getGroupManager().delete((Group) entity);
                                } else if (entity instanceof User) {
                                    identityProvider.getUserManager().delete((User) entity);
                                }
                            } catch (DeleteException e) {
                                return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>left(e));
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
                                return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>left(new DeleteException("Cannot find interface tag: " + ((InterfaceTag) entity).getName(), e)));
                            }
                        } else if (entity instanceof Role && !((Role) entity).isUserCreated()) {
                            return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>right(new IncorrectMappingInstructionsException(mapping, "Cannot delete system role '" + ((Role) entity).getName() + "' with Id " + entity.getId())));
                        } else if (entity instanceof Folder && Goid.equals(Folder.ROOT_FOLDER_ID, ((Folder) entity).getGoid())) {
                            return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>right(new IncorrectMappingInstructionsException(mapping, "Cannot delete the root folder")));
                        } else if (entity instanceof Policy) {
                            //validate that it is not used by things that have not been deleted.
                            final Set<Policy> usages = policyCache.findUsages(((Policy) entity).getGoid());
                            final Policy policyUsed = Functions.grepFirst(usages, new Functions.Unary<Boolean, Policy>() {
                                @Override
                                public Boolean call(Policy policy) {
                                    return !deletedPolicyIds.contains(policy.getGoid());
                                }
                            });
                            if(policyUsed != null){
                                return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>right(new IncorrectMappingInstructionsException(mapping, "Could not delete policy, it is being used by: " + EntityHeaderUtils.fromEntity(policyUsed).toStringVerbose())));
                            }
                            try {
                                policyManager.deleteWithoutValidation((Policy) entity);
                                deletedPolicyIds.add(((Policy) entity).getGoid());
                            } catch (DeleteException e) {
                                return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>right(new IncorrectMappingInstructionsException(mapping, "Could not delete policy", e)));
                            }
                        } else if (entity instanceof PublishedService) {
                            try {
                                serviceManager.delete((PublishedService) entity);
                                deletedPolicyIds.add(((PublishedService) entity).getPolicy().getGoid());
                            } catch (DeleteException e) {
                                return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>right(new IncorrectMappingInstructionsException(mapping, "Could not delete service", e)));
                            }
                        } else {
                            try {
                                entityCrud.delete(entity);
                            } catch (DeleteException e) {
                                return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>right(new IncorrectMappingInstructionsException(mapping, "Could not delete entity", e)));
                            }
                        }
                    } finally {
                        transactionStatus.flush();
                    }
                } catch (DataIntegrityViolationException e) {
                    return Either.left(Either.<DeleteException, IncorrectMappingInstructionsException>right(new IncorrectMappingInstructionsException(mapping, "Could not delete entity. Mappings are likely out of order", e)));
                }
                return Either.right(EntityHeaderUtils.fromEntity(entity));
            }
        });
        if (headerOrException.isRight()) {
            return headerOrException.right();
        } else if (headerOrException.left().isLeft()) {
            throw headerOrException.left().left();
        } else {
            throw headerOrException.left().right();
        }
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
            throw new IncorrectMappingInstructionsException(mapping, "Cannot perform action " + mapping.getMappingAction() +
                    " because there is no entity of type " + mapping.getSourceEntityHeader().getType() + " with id: " +
                    mapping.getSourceEntityHeader().getGoid() + " in the bundle. Please specify " +
                    EntityMappingInstructions.MappingAction.NewOrExisting + " or " + EntityMappingInstructions.MappingAction.Ignore + " instead");
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

        //don't replace dependencies on generic entities. This will happen after all other dependencies are replaced in order to allow for circular dependencies
        if (!(entityContainer.getEntity() instanceof GenericEntity)) {
            //not replace dependencies for assertions
            dependencyAnalyzer.replaceDependencies(entityContainer.getEntity(), resourceMapping, false);
        }

        //create/save dependent entities
        final EntityMappingInstructions.TargetMapping targetMapping = mapping.getTargetMapping();
        beforeCreateOrUpdateEntities(entityContainer, existingEntity, (targetMapping != null && EntityMappingInstructions.TargetMapping.Type.ID.equals(targetMapping.getType())) ? id : null, resourceMapping);

        //if it is a mapping by name and the mapped name is set it should be preserved here. Or if the mapped GUID is set it should be preserved.
        if (targetMapping != null && targetMapping.getTargetID() != null) {
            final Entity entityContainerEntity = entityContainer.getEntity();
            switch (targetMapping.getType()) {
                case NAME:
                    if (entityContainerEntity instanceof NameableEntity) {
                        ((NameableEntity) entityContainerEntity).setName(targetMapping.getTargetID());
                    } else {
                        throwIncorrectMappingInstructionsExceptionDueToIncorrectEntityType(mapping, "name");
                    }
                    break;
                case GUID:
                    if (entityContainerEntity instanceof GuidEntity) {
                        ((GuidEntity) entityContainerEntity).setGuid(targetMapping.getTargetID());
                    } else {
                        throwIncorrectMappingInstructionsExceptionDueToIncorrectEntityType(mapping, "guid");
                    }
                    break;
                case ROUTING_URI:
                    if (entityContainerEntity instanceof PublishedService) {
                        ((PublishedService) entityContainerEntity).setRoutingUri(targetMapping.getTargetID());
                    } else {
                        throwIncorrectMappingInstructionsExceptionDueToIncorrectEntityType(mapping, "routing uri");
                    }
                    break;
                case PATH:
                    if (entityContainerEntity instanceof HasFolder) {
                        final String path = targetMapping.getTargetID(); // Note: path is not null.
                        final Pair<String, String> pair = PathUtils.parseEntityPathIntoFolderPathAndEntityName(path);
                        final String folderPath = pair.left;
                        final String entityName = pair.right;

                        // If either is null, then the target path is incorrect.
                        if (folderPath == null || entityName == null) {
                            throw new IncorrectMappingInstructionsException(mapping, "The target path is not a valid entity path.");
                        }

                        Folder folder;
                        try {
                            folder = folderManager.findByPath(folderPath);
                        } catch (final FindException e) {
                            throw new IncorrectMappingInstructionsException(mapping, "Error finding the folder by a path '" + folderPath + "'.");
                        }
                        if (folder == null) {
                            folder = folderManager.createPath(folderPath);
                            logger.fine("The folder '" + folderPath + "' did not exist and has been created.");
                        }

                        // Preserve the parent folder for this entity
                        ((HasFolder) entityContainerEntity).setFolder(folder);

                        // Preserve the name for this entity
                        if (entityContainerEntity instanceof NameableEntity) {
                            ((NameableEntity) entityContainerEntity).setName(entityName);
                        }
                    } else {
                        throwIncorrectMappingInstructionsExceptionDueToIncorrectEntityType(mapping, "path");
                    }
                    break;
            }
        }

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
                                    if (user instanceof InternalUser){
                                        final InternalUser internalUser = (InternalUser) user;
                                        if (existingEntity instanceof InternalUser) {
                                            internalUser.setPasswordChangesHistory(((InternalUser) existingEntity).getPasswordChangesHistory());
                                        }
                                        passwordEnforcerManager.setUserPasswordPolicyAttributes((InternalUser)user, true);
                                    }
                                    final String userId = userManager.save(id == null ? null : Goid.parseGoid(id), user, null);
                                    ((UserBean) entityContainer.getEntity()).setUniqueIdentifier(userId);
                                } else {
                                    final UserBean userBean = (UserBean) entityContainer.getEntity();
                                    userBean.setUniqueIdentifier(existingEntity.getId());

                                    final User user = userManager.reify(userBean);
                                    if(user instanceof InternalUser && existingEntity instanceof InternalUser){
                                        ((InternalUser) user).setPasswordChangesHistory(((InternalUser) existingEntity).getPasswordChangesHistory());
                                        passwordEnforcerManager.setUserPasswordPolicyAttributes((InternalUser)user, true);
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

    private void throwIncorrectMappingInstructionsExceptionDueToIncorrectEntityType(@NotNull final EntityMappingInstructions mapping, @NotNull String mapByStr) throws IncorrectMappingInstructionsException {
        throw new IncorrectMappingInstructionsException(mapping, "Attempting to map an entity by " + mapByStr + " that cannot be mapped by " + mapByStr + ".");
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
    private void beforeCreateOrUpdateEntities(@NotNull final EntityContainer entityContainer, @Nullable final Entity existingEntity, @Nullable final String targetId, @NotNull final Map<EntityHeader, EntityHeader> replacementMap) throws ObjectModelException, CannotReplaceDependenciesException {
        final Entity entity = entityContainer.getEntity();

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
        } else if (entity instanceof EncapsulatedAssertionConfig) {
            //need to find the real policy to attach to the encass so that it can be properly updated. The policy id here should be correct. It will already have been properly mapped.
            final EncapsulatedAssertionConfig encassConfig = ((EncapsulatedAssertionConfig) entity);
            final Policy encassPolicy = ((EncapsulatedAssertionConfig) entity).getPolicy();
            if (encassPolicy != null) {
                final Policy policyFound = policyManager.findByPrimaryKey(encassPolicy.getGoid());
                encassConfig.setPolicy(policyFound);
            }
        } else if (entity instanceof Role && !((Role) entity).isUserCreated()) {
            //if this is not a user created role copy the permissions for the target to updating role
            if(existingEntity != null){
                //set the role permissions to those of the target role
                final Set<Permission> permissions = ((Role)entity).getPermissions();
                permissions.clear();
                for(final Permission permission : ((Role)existingEntity).getPermissions()){
                    permissions.add(permission);
                }

                //if this is the admin role we need to add the admin tag
                if(Role.Tag.ADMIN.equals(((Role) existingEntity).getTag())){
                    ((Role) entity).setTag(Role.Tag.ADMIN);
                }
            } else {
                // TODO somehow reuse logic in RbacRoleResourceFactory
                // this is a new role, must be 'userCreated'
                ((Role) entity).setUserCreated(true);
            }
        } else if (entity instanceof Alias) {
            //need to check that the alias will not be created in the same folder as the policy or service it is aliasing. Or that is it is created in a folder that already has an alias for that policy or service.
            //this checks if it is to be created in a folder with the aliased service or policy
            final EntityHeader serviceOrPolicyHeader = entityCrud.findHeader(entity instanceof PublishedServiceAlias ? EntityType.SERVICE : EntityType.POLICY, ((Alias) entity).getEntityGoid());
            if (serviceOrPolicyHeader instanceof OrganizationHeader) {
                if (Goid.equals(((OrganizationHeader) serviceOrPolicyHeader).getFolderId(), ((Alias) entity).getFolder().getGoid())) {
                    throw new DuplicateObjectException("Cannot create alias in the same folder as the aliased policy or service");
                }
            } else if (serviceOrPolicyHeader == null) {
                final String serviceOrPolicy = entity instanceof PublishedServiceAlias ? "service" : "policy";
                //note this needs to be a ConstraintViolationException and not a FindException so the the proper mapping is returned. If it is a FindException the mapping error type for the alias is TargetNotFound but we actually want InvalidResource
                throw new ConstraintViolationException("Could not find the " + serviceOrPolicy + " for alias. " + serviceOrPolicy + " id: " + ((Alias) entity).getEntityGoid());
            } else {
                throw new IllegalStateException("A policy or service header is expected to be an OrganizationHeader but it was not. Header: " + serviceOrPolicyHeader);
            }

            // This checks if it is to be created in a folder with another alias for the same service or policy
            final AliasManager aliasManager = entity instanceof PublishedServiceAlias ? serviceAliasManager : policyAliasManager;
            final Alias checkAlias = aliasManager.findAliasByEntityAndFolder(((Alias) entity).getEntityGoid(), ((Alias) entity).getFolder().getGoid());
            if (checkAlias != null && (existingEntity == null || !StringUtils.equals(existingEntity.getId(), checkAlias.getId()))) {
                throw new DuplicateObjectException("Cannot create alias in the same folder as an alias for the same aliased policy or service");
            }
        } else if (entity instanceof SsgKeyEntry && targetId != null) {
            //need to replace the alias and keystore id if a target id is specified
            final SsgKeyEntry ssgKeyEntry = ((SsgKeyEntry) entity);
            final String[] keyParts = targetId.split(":");
            if(keyParts.length != 2 || keyParts[1] == null || keyParts[1].isEmpty() || keyParts[0] == null) {
                throw new IllegalStateException("An id for a private key is not valid: " + targetId);
            }
            ssgKeyEntry.setAlias(keyParts[1]);
            ssgKeyEntry.setKeystoreId(Goid.parseGoid(keyParts[0]));
        } else if (entity instanceof PolicyBackedService && existingEntity != null) {
            //TODO: This logic is duplicated in com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyBackedServiceAPIResourceFactory.beforeUpdateEntity()
            final PolicyBackedService newPolicyBackedService = (PolicyBackedService) entity;
            final PolicyBackedService oldPolicyBackedService = (PolicyBackedService) existingEntity;
            //This is needed in order to avoid hibernate issues related to updating operations to a PolicyBackedService
            //update to existing operations to match the updated ones.
            final Set<PolicyBackedServiceOperation> operations = oldPolicyBackedService.getOperations();
            final Iterator<PolicyBackedServiceOperation> operationsIterator = operations.iterator();
            while(operationsIterator.hasNext()){
                //This will update the existing operations to match the updated ones. And remove existing operations that are no longer in the updated PBS
                final PolicyBackedServiceOperation operation = operationsIterator.next();
                //Find the new operation with the same operation name a this existing operation
                final PolicyBackedServiceOperation newOperation = Functions.grepFirst(newPolicyBackedService.getOperations(), new Functions.Unary<Boolean, PolicyBackedServiceOperation>() {
                    @Override
                    public Boolean call(PolicyBackedServiceOperation policyBackedServiceOperation) {
                        return StringUtils.equals(policyBackedServiceOperation.getName(), operation.getName());
                    }
                });
                if(newOperation != null) {
                    //updated the existing operation policy id to match the new one
                    // But if the new one is a stale id, then just ignore it.
                    final Goid newPolicyGoid = newOperation.getPolicyGoid();
                    if (policyManager.findByPrimaryKey(newPolicyGoid) != null) {
                        operation.setPolicyGoid(newPolicyGoid);
                    }
                } else {
                    //remove the existing operation since it is not available in the new PBS.
                    operationsIterator.remove();
                }
            }
            //This will add any new operations to the list of existing operations.
            for(final PolicyBackedServiceOperation operation : newPolicyBackedService.getOperations()){
                //Find the existing operation with the same operation name a this new operation
                final PolicyBackedServiceOperation oldOperation = Functions.grepFirst(oldPolicyBackedService.getOperations(), new Functions.Unary<Boolean, PolicyBackedServiceOperation>() {
                    @Override
                    public Boolean call(PolicyBackedServiceOperation policyBackedServiceOperation) {
                        return StringUtils.equals(policyBackedServiceOperation.getName(), operation.getName());
                    }
                });
                //If there is no existing operation with the same name then add a new one.
                if(oldOperation == null){
                    operations.add(operation);
                }
            }
            newPolicyBackedService.setOperations(operations);
        } else if (entity instanceof ScheduledTask && existingEntity != null) {
            // If the new policy goid a stale id, then just use the existing policy goid.
            final Goid newPolicyGoid = ((ScheduledTask) entity).getPolicyGoid();
            if (policyManager.findByPrimaryKey(newPolicyGoid) == null) {
                ((ScheduledTask)entity).setPolicyGoid(((ScheduledTask) existingEntity).getPolicyGoid());
            }
        }
        //if this entity has a folder and it is mapped to an existing entity then ignore the given folderID and use the folderId of the existing entity.
        if(entity instanceof HasFolder && existingEntity != null) {
            if(existingEntity instanceof HasFolder){
                ((HasFolder)entity).setFolder(((HasFolder)existingEntity).getFolder());
            } else {
                //this should never happen
                throw new IllegalStateException("A folderable entity was mapped to an entity that is not folderable: " + existingEntity.getClass());
            }
        }
        //if this is an entity that get referenced by name by others and  the existing entity exists then preserve the existing entity name
        if((entity instanceof JdbcConnection ||
            entity instanceof ClusterProperty ||
            entity instanceof SecurePassword ||
            entity instanceof InterfaceTag ||
            (entity instanceof Role &&  !((Role)entity).isUserCreated())) &&
                existingEntity != null ) {
            if(existingEntity instanceof NameableEntity){
                ((NameableEntity) entity).setName(((NameableEntity) existingEntity).getName());
            } else {
                //this should never happen
                throw new IllegalStateException("A NameableEntity entity was mapped to an entity that is not NameableEntity: " + existingEntity.getClass());
            }
        }
        //if this is a GuidEntity and the existing entity exists then preserve the existing entity guid
        if(entity instanceof GuidEntity && existingEntity != null ) {
            if(existingEntity instanceof GuidEntity){
                ((GuidEntity) entity).setGuid(((GuidEntity) existingEntity).getGuid());
            } else {
                //this should never happen
                throw new IllegalStateException("A GuidEntity entity was mapped to an entity that is not GuidEntity: " + existingEntity.getClass());
            }
        }
        //if this is a user and the existing entity exists then preserve the existing entity login
        if(entity instanceof UserBean && existingEntity != null ) {
            if(existingEntity instanceof User){
                ((UserBean) entity).setLogin(((User) existingEntity).getLogin());
                if(existingEntity instanceof InternalUser) {
                    //for internal users the name and login should match
                    ((UserBean) entity).setName(((User) existingEntity).getName());
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
     * TODO: extract this logic into its own 'existing entity finder' class
     *
     * @param mapping         The entity mapping instructions
     * @param entity          The entity to import
     * @param bundle          The entity bundle to import
     * @param resourceMapping This is used to get any already mapped entities that could be needed in locating the given
     *                        entity.
     * @return The existing entity that this entity should be mapped to or null if there is no existing entity to map
     * this entity to.
     * @throws BundleImportException This is thrown if the entity mapping instructions are not properly set or there is
     *                               an error locating an identity provider when mapping identities
     */
    @Nullable
    private Entity locateExistingEntity(@NotNull final EntityMappingInstructions mapping, @Nullable final Entity entity, @NotNull final EntityBundle bundle,
                                        @NotNull final Map<EntityHeader, EntityHeader> resourceMapping) throws BundleImportException {
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
        //we are not making any changes to the database, just searching for entities.
        tt.setReadOnly(true);
        return Eithers.extract(tt.execute(new TransactionCallback<Either<BundleImportException, Option<Entity>>>() {
            @Override
            public Either<BundleImportException, Option<Entity>> doInTransaction(final TransactionStatus transactionStatus) {
                try {
                    //get the mapping target identifier
                    final String mappingTarget;
                    try {
                        mappingTarget = getMapTo(mapping, resourceMapping, bundle);
                    } catch (final IncorrectMappingInstructionsException e) {
                        return Either.left(e);
                    }
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
                    } else if (EntityType.SERVER_MODULE_FILE.equals(mapping.getSourceEntityHeader().getType()) && mapping.getTargetMapping() != null && EntityMappingInstructions.TargetMapping.Type.MODULE_SHA265.equals(mapping.getTargetMapping().getType())) {
                        final ServerModuleFile moduleFileEntity = (ServerModuleFile)entity;
                        if (moduleFileEntity == null){
                            return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Attempting to map a ServerModuleFile by it's moduleSha256 but the ServerModuleFile is not in the bundle"));
                        } else if (StringUtils.isBlank(moduleFileEntity.getModuleSha256())) {
                            return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Attempting to map a ServerModuleFile by it's moduleSha256 but the ServerModuleFile moduleSha256 is empty"));
                        } else {
                            try {
                                resource = serverModuleFileManager.findModuleWithSha256(moduleFileEntity.getModuleSha256());
                            } catch (Exception e) {
                                return Either.<BundleImportException, Option<Entity>>left(new TargetNotFoundException(mapping, "Error finding ServerModuleFile by it's moduleSha256: " + ExceptionUtils.getMessage(e)));
                            }
                        }
                    } else if (EntityType.SSG_KEY_ENTRY.equals(mapping.getSourceEntityHeader().getType()) && mapping.getTargetMapping() != null && EntityMappingInstructions.TargetMapping.Type.NAME.equals(mapping.getTargetMapping().getType())) {
                        if(entity == null) {
                            return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Attempting to map a private key by it's name but the private key is not in the bundle"));
                        } else {
                            try {
                                resource = keyStoreManager.lookupKeyByKeyAlias(mappingTarget, ((SsgKeyEntry)entity).getKeystoreId());
                            } catch (KeyStoreException e) {
                                return Either.<BundleImportException, Option<Entity>>left(new TargetNotFoundException(mapping, "Error looking up private key. Message: " + ExceptionUtils.getMessageWithCause(e)));
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
                                case ROUTING_URI: {
                                    //Find the entity by its routing uri
                                    final Collection<PublishedService> list = serviceManager.findByRoutingUri(mappingTarget);
                                    if (list.isEmpty()) {
                                        resource = null;
                                    } else if (list.size() > 1) {
                                        return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Found multiple possible target entities found with routing URI: " + mappingTarget));
                                    } else {
                                        resource = list.iterator().next();
                                    }
                                    break;
                                }
                                case PATH: {
                                    // TODO extract this to a method
                                    //Find the entity by its path
                                    if (EntityType.FOLDER.equals(mapping.getSourceEntityHeader().getType())) {
                                        resource = folderManager.findByPath(mappingTarget);
                                    } else {
                                        // Note: mappingTarget is not null.
                                        final Pair<String, String> pair = PathUtils.parseEntityPathIntoFolderPathAndEntityName(mappingTarget);
                                        final String folderPath = pair.left;
                                        final String entityName = pair.right;

                                        // If either is null, then resource cannot be found.
                                        if (folderPath == null || entityName == null) {
                                            resource = null;
                                            logger.fine("The entity cannot be found, since its entity path is invalid.");
                                            break;
                                        }

                                        final Folder folder;
                                        try {
                                            folder = folderManager.findByPath(folderPath);
                                        } catch (final FindException fe) {
                                            // If any folder on the folder path is not found, it implies the south entity cannot be found in the target gateway.
                                            resource = null;
                                            logger.fine("The entity '" + entityName + "' cannot be found by its path '" + folderPath + "'.");
                                            break;
                                        }
                                        // If folder path does not exist, then treat this case as resource not found.
                                        if (folder == null) {
                                            resource = null;
                                            logger.fine("The entity '" + entityName + "' cannot be found due to its folder path '" + folderPath + "' not found.");
                                            break;
                                        }

                                        final List<Entity> matches = new ArrayList<>();
                                        if (!EntityType.POLICY_ALIAS.equals(mapping.getSourceEntityHeader().getType()) &&
                                                !EntityType.SERVICE_ALIAS.equals(mapping.getSourceEntityHeader().getType())) {
                                            // lookup by parent folder and name
                                            final List<? extends Entity> matchedFolderAndName = entityCrud.findAll(
                                                    mapping.getSourceEntityHeader().getType().getEntityClass(),
                                                    CollectionUtils.MapBuilder.<String, List<Object>>builder().put("folder",
                                                            Arrays.<Object>asList(folder)).put("name", Arrays.asList(entityName)).map(),
                                                    0, -1, null, null);
                                            matches.addAll(matchedFolderAndName);
                                        } else {
                                            // lookup by alias children in the folder
                                            final List<? extends Entity> aliasChildren = entityCrud.findAll(
                                                    mapping.getSourceEntityHeader().getType().getEntityClass(),
                                                    CollectionUtils.MapBuilder.<String, List<Object>>builder().put("folder",
                                                    Arrays.asList(folder)).map(), 0, -1, null, null);
                                            for (final Entity child : aliasChildren) {
                                                if (child instanceof Alias) {
                                                    final Alias alias = (Alias) child;
                                                    if (alias.getEntityGoid() != null && alias.getEntityType() != null) {
                                                        final String aliasedEntityName = entityName.replaceAll(" alias", "");
                                                        final EntityHeader aliasedEntity = entityCrud.findHeader(alias.getEntityType(), alias.getEntityGoid());
                                                        if (aliasedEntity != null && aliasedEntity.getName().equals(aliasedEntityName)) {
                                                            matches.add(child);
                                                            break; // can't have more than one alias with the same name under the same folder
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (matches.isEmpty()) {
                                            resource = null;
                                        } else if (matches.size() > 1) {
                                            return Either.left(new IncorrectMappingInstructionsException(mapping, "Found multiple possible target entities found by path: " + mappingTarget));
                                        } else {
                                            resource = matches.iterator().next();
                                        }
                                    }
                                    break;
                                }
                                case MAP_BY_ROLE_ENTITY: {
                                    return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Specified mapping by role entity but the source entity is not appropriate. The source entity should be a role that is not user created and specifies both entity id and entity type" ));
                                }
                                case MODULE_SHA265: {
                                    return Either.<BundleImportException, Option<Entity>>left(new IncorrectMappingInstructionsException(mapping, "Specified mapping moduleSha256 but the source entity is not appropriate. The source entity should be a ServerModuleFile" ));
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
     * Gets the mapping target is from the mapping instructions. This could be id, name, guid, routing uri, role entity, or module sha265.
     *
     * @param mapping The mapping instructions to find the target mapping id from.
     * @return The target mapping id
     * @throws IncorrectMappingInstructionsException This is thrown if the mapping instructions are incorrect and a
     *                                               target mapping id cannot be found
     */
    @NotNull
    private String getMapTo(@NotNull final EntityMappingInstructions mapping, @NotNull final Map<EntityHeader, EntityHeader> resourceMapping, final @NotNull EntityBundle bundle) throws IncorrectMappingInstructionsException {
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
            } else if (EntityMappingInstructions.TargetMapping.Type.ROUTING_URI.equals(type) && mapping.getSourceEntityHeader() instanceof ServiceHeader && ((ServiceHeader) mapping.getSourceEntityHeader()).getRoutingUri() != null) {
                // mapping by routing uri so get the target routing uri from the source.
                targetMapTo = ((ServiceHeader) mapping.getSourceEntityHeader()).getRoutingUri();
            } else if (EntityMappingInstructions.TargetMapping.Type.PATH.equals(type) && mapping.getSourceEntityHeader() instanceof HasFolderId) {
                if (((HasFolderId) mapping.getSourceEntityHeader()).getFolderId() != null) {
                    // mapping by path so get the target path from the source.
                    final EntityContainer container = bundle.getEntity(mapping.getSourceEntityHeader().getStrId(), mapping.getSourceEntityHeader().getType());
                    if (container != null && container.getEntity() instanceof HasFolder) {
                        final HasFolder hasFolder = (HasFolder) container.getEntity();
                        Folder parent = hasFolder.getFolder();

                        if (parent != null) {
                            // ensure the entity's parent is up-to-date as it can affect determining its path
                            EntityHeader parentHeader = EntityHeaderUtils.fromEntity(parent);
                            if (resourceMapping.containsKey(parentHeader)) {
                                // parent was previously mapped, make sure we have the mapped parent
                                parentHeader = resourceMapping.get(parentHeader);
                            }
                            try {
                                parent = folderManager.findByHeader(parentHeader);
                                if (parent != null) {
                                    hasFolder.setFolder(parent);
                                } else {
                                    logger.log(Level.WARNING, "Unable to find parent folder: " + parentHeader);
                                }
                            } catch (final FindException e) {
                                logger.log(Level.WARNING, "Error retrieving parent folder: " + e.getMessage(), ExceptionUtils.getDebugException(e));
                            }
                        }

                        final ArrayList<String> names = new ArrayList<>();
                        names.add(mapping.getSourceEntityHeader().getName());
                        while (parent != null && !parent.getGoid().equals(Folder.ROOT_FOLDER_ID) && parent.getName() != null) {
                            names.add(0, parent.getName());
                            parent = parent.getFolder();
                        }
                        String escapedPath = PathUtils.getEscapedPathString(names.toArray(new String[names.size()]));
                        if (!escapedPath.startsWith("/")) {
                            escapedPath = "/" + escapedPath;
                        }
                        targetMapTo = escapedPath;
                    } else {
                        throw new IncorrectMappingInstructionsException(mapping, "Mapping by path but entity is not a folderable type");
                    }
                } else {
                    // map to root folder
                    targetMapTo = "/";
                }
            } else if (EntityMappingInstructions.TargetMapping.Type.MAP_BY_ROLE_ENTITY.equals(type)) {
                // set the target mapping to the id of the role, this should be the default.
                targetMapTo = mapping.getSourceEntityHeader().getStrId();
            } else if (EntityMappingInstructions.TargetMapping.Type.MODULE_SHA265.equals(type)) {
                // set the target mapping to the id of the server module file, this should be the default.
                targetMapTo = mapping.getSourceEntityHeader().getStrId();
            } else {
                //cannot find a target id.
                throw new IncorrectMappingInstructionsException(mapping, "Mapping by " + type + " but could not deduce a target " + type + " to map to from the given bundle. Please specify a target " + type + " to map to");
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