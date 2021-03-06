package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.APIUtilityLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.URLAccessibleLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.bundling.EntityBundle;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.bundling.EntityMappingInstructions;
import com.l7tech.server.bundling.EntityMappingResult;
import com.l7tech.server.bundling.exceptions.IncorrectMappingInstructionsException;
import com.l7tech.server.bundling.exceptions.TargetExistsException;
import com.l7tech.server.bundling.exceptions.TargetNotFoundException;
import com.l7tech.server.bundling.exceptions.TargetReadOnlyException;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * This a the bundle transformer. It will transform a bundle to an internal EntityBundle and back. It is also used to
 * transform mappings
 */
@SuppressWarnings("unchecked")
@Component
public class BundleTransformer implements APITransformer<Bundle, EntityBundle> {

    //Theses are the different properties that can be in a Mapping
    private static final String FAIL_ON_NEW = "FailOnNew";
    private static final String FAIL_ON_EXISTING = "FailOnExisting";
    static final String MAP_TO = "MapTo";
    static final String MAP_BY = "MapBy";
    @Inject
    private APIUtilityLocator apiUtilityLocator;
    @Inject
    private URLAccessibleLocator urlAccessibleLocator;

    @NotNull
    @Override
    public String getResourceType() {
        return "BUNDLE";
    }

    /**
     * Converts a Entity bundle into a Bundle
     *
     * @param entityBundle The entity bundle to convert to a Bundle
     * @param secretsEncryptor for encrypting and including the passwords in the bundle. Null for not encrypting and including.
     * @return The Bundle
     */
    @NotNull
    public Bundle convertToMO(@NotNull final EntityBundle entityBundle, SecretsEncryptor secretsEncryptor){
        return convertToMO(entityBundle, secretsEncryptor, Collections.<EntityHeader>emptyList());
    }

    /**
     * Converts a Entity bundle into a Bundle
     *
     * @param entityBundle The entity bundle to convert to a Bundle
     * @param secretsEncryptor for encrypting and including the passwords in the bundle. Null for not encrypting and including.
     * @return The Bundle
     */
    @NotNull
    public Bundle convertToMO(@NotNull final EntityBundle entityBundle, SecretsEncryptor secretsEncryptor, @NotNull final List<EntityHeader> forceFailOnNew) {
        final ArrayList<Item> items = new ArrayList<>();
        final ArrayList<Mapping> mappings = new ArrayList<>();

        for (final EntityMappingInstructions entityMappingInstruction : entityBundle.getMappingInstructions()) {
            final EntityContainer entityResource = getEntityContainerFromBundle(entityMappingInstruction, entityBundle);
            if (entityResource != null) {
                //Get the transformer for the entity so that it can be converted.
                final EntityAPITransformer transformer = apiUtilityLocator.findTransformerByResourceType(entityMappingInstruction.getSourceEntityHeader().getType().toString());
                if (transformer == null) {
                    throw new IllegalStateException("Cannot locate a transformer for entity type: " + entityMappingInstruction.getSourceEntityHeader().getType());
                }
                //get the MO from the entity
                final Object mo;
                //include certificates for users
                if(entityResource.getEntity() instanceof User) {
                    mo = ((UserTransformer)transformer).convertToMO((User) entityResource.getEntity(), secretsEncryptor, true);
                } else if(entityResource.getEntity() instanceof ServerModuleFile) {
                    // include ServerModuleFile bytes
                    mo = ((ServerModuleFileTransformer)transformer).convertToMO((ServerModuleFile) entityResource.getEntity(), secretsEncryptor, true);
                } else {
                    mo = transformer.convertToMO(entityResource.getEntity(), secretsEncryptor);
                }
                //remove the permissions from system created roles in the bundle (makes the bundle easier to read as these permissions are not required.)
                if(mo instanceof RbacRoleMO && !((RbacRoleMO)mo).isUserCreated()){
                    ((RbacRoleMO)mo).setPermissions(null);
                }
                //create an item from the mo
                final Item<?> item = transformer.convertToItem(mo);
                //add the item to the bundle items list
                items.add(item);
            }

            //convert the mapping
            mappings.add(convertEntityMappingInstructionsToMapping(entityMappingInstruction, forceFailOnNew));
        }

        final Bundle bundle = ManagedObjectFactory.createBundle();
        bundle.setReferences(items);
        bundle.setMappings(mappings);
        return bundle;
    }

    /**
     * Returns an entity container form the entity bundle using the given mapping
     *
     * @param mapping The mapping to find the entity container for
     * @param bundle  The bundle to find the entity container in
     * @return The entity container for this mapping. Or null if there isn't one in the bundle.
     */
    //TODO: This is shared with EntityBundleImporterImpl need to make it common
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
     * This converts a bundle to an entity bundle.
     *
     * @param bundle The bundle to convert
     * @param secretsEncryptor
     * @return The entity bundle created from the given bundle
     * @throws ResourceFactory.InvalidResourceException
     */
    @Override
    @NotNull
    public EntityBundle convertFromMO(@NotNull final Bundle bundle, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(bundle, true, secretsEncryptor);
    }

    /**
     * This converts a list of bundles to a list of entity bundles.
     *
     * @param bundles The bundles to convert
     * @param secretsEncryptor
     * @return A list of the entity bundles created from the given bundles
     * @throws ResourceFactory.InvalidResourceException
     */
    @NotNull
    public List<EntityBundle> convertFromMO(@NotNull final BundleList bundles, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        final List<Bundle> bundleList = bundles.getBundles();
        final List<EntityBundle> entityBundles = new ArrayList<>(bundleList.size());

        for (final Bundle bundle: bundleList) {
            entityBundles.add(convertFromMO(bundle, secretsEncryptor));
        }

        return entityBundles;
    }

    /**
     * This converts a bundle to an entity bundle.
     *
     * @param bundle The bundle to convert
     * @param strict This have no effect for bundles
     * @param secretsEncryptor
     * @return The entity bundle created from the given bundle
     * @throws ResourceFactory.InvalidResourceException
     */
    @Override
    @NotNull
    public EntityBundle convertFromMO(@NotNull final Bundle bundle, final boolean strict, final SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        // Convert all the MO's in the bundle to entities

        final List<EntityContainer> entityContainers = bundle.getReferences() == null ? Collections.<EntityContainer>emptyList() : Functions.map(bundle.getReferences(), new Functions.UnaryThrows<EntityContainer, Item, ResourceFactory.InvalidResourceException>() {
            @Override
            public EntityContainer call(Item item) throws ResourceFactory.InvalidResourceException {
                final EntityAPITransformer transformer = apiUtilityLocator.findTransformerByResourceType(item.getType());
                if (transformer == null) {
                    throw new IllegalStateException("Cannot locate a transformer for entity type: " + item.getType());
                }
                //cannot be strict here because there will be many cases where the reference entities in the mo's do not exist on the gateway
                return (EntityContainer) transformer.convertFromMO(item.getContent(), false, secretsEncryptor);
            }
        });

        //Create a map of the entities so that they can quickly be referenced.
        final Map<Pair<String, EntityType>, EntityContainer> entityContainerMap = Functions.toMap(entityContainers, new Functions.Unary<Pair<Pair<String, EntityType>, EntityContainer>, EntityContainer>() {
            @Override
            public Pair<Pair<String, EntityType>, EntityContainer> call(EntityContainer item) {
                return new Pair<Pair<String, EntityType>, EntityContainer>(item.getId(), item);
            }
        });

        //Validate entityMap has the same number of elements as there are entityContainers
        assert entityContainerMap.size() <= entityContainers.size();
        if (entityContainerMap.size() < entityContainers.size()) {
            List<EntityContainer> duplicates = new ArrayList<>(entityContainers);
            duplicates.removeAll(entityContainerMap.values());
            StringBuilder error = new StringBuilder("Could not uniquely map EntityContainers by their id's. Found these duplicates ids:\n");
            for(EntityContainer e : duplicates) {
                error.append(e.getId());
                error.append("\n");
            }
            throw new IllegalStateException(error.toString());
        }

        //convert the mappings to entityMapping instruction.
        final List<EntityMappingInstructions> mappingInstructions = Functions.map(bundle.getMappings(), new Functions.UnaryThrows<EntityMappingInstructions, Mapping, ResourceFactory.InvalidResourceException>() {
            @Override
            public EntityMappingInstructions call(Mapping mapping) throws ResourceFactory.InvalidResourceException {
                return convertEntityMappingInstructionsFromMappingAndEntity(mapping, entityContainerMap);
            }
        });

        // not transform dependency results, not used by import
        return new EntityBundle(bundle.getName(), entityContainers, mappingInstructions, new ArrayList<DependencySearchResults>() );
    }

    @Override
    @NotNull
    public Item<Bundle> convertToItem(@NotNull Bundle bundle) {
        return new ItemBuilder<Bundle>("Bundle", "BUNDLE")
                .setContent(bundle)
                .build();
    }

    /**
     * This will update the mappings list given applying the mappings results in the {@link
     * com.l7tech.server.bundling.EntityMappingResult} list given. This will also set the target URL on the mappings
     *
     * @param mappings          The mappings to update
     * @param mappingsPerformed The entity mapping results to update the mappings with
     * @return The updated mappings list
     */
    @NotNull
    public List<Mapping> updateMappings(@NotNull final List<Mapping> mappings, @NotNull final List<EntityMappingResult> mappingsPerformed) {
        //create a mappings map so that they can be easily retrieved.
        final Map<Pair<String, EntityType>, Mapping> mappingsMap = Functions.toMap(mappings, new Functions.Unary<Pair<Pair<String, EntityType>, Mapping>, Mapping>() {
            @Override
            public Pair<Pair<String, EntityType>, Mapping> call(Mapping mapping) {
                return new Pair<>(new Pair<>(mapping.getSrcId(), EntityType.valueOf(mapping.getType())), mapping);
            }
        });

        final List<Mapping> updatedMappings = new ArrayList<>();
        for (final EntityMappingResult entityMappingResult : mappingsPerformed) {
            //get the mappings for this EntityMappingResult
            final Mapping mapping = mappingsMap.get(new Pair<>(EntityType.ASSERTION_ACCESS.equals(entityMappingResult.getSourceEntityHeader().getType()) ? entityMappingResult.getSourceEntityHeader().getName() : entityMappingResult.getSourceEntityHeader().getStrId(), entityMappingResult.getSourceEntityHeader().getType()));
            //get the updated mapping
            final Mapping mappingUpdated = convertMappingAndEntityMappingResultToMapping(mapping, entityMappingResult);
            //set the target url
            if (entityMappingResult.getTargetEntityHeader() != null) {
                final URLAccessible urlAccessible = urlAccessibleLocator.findByEntityType(mapping.getType());
                mappingUpdated.setTargetUri(urlAccessible.getUrl(entityMappingResult.getTargetEntityHeader()));
            }
            updatedMappings.add(mappingUpdated);
        }
        return updatedMappings;
    }

    /**
     * Converts an {@link com.l7tech.server.bundling.EntityMappingInstructions} object to a {@link
     * com.l7tech.gateway.api.Mapping} object
     *
     * @param entityMappingInstructions The entity mapping instructions to convert
     * @param forceFailOnNew            These items will have fail on new set to true
     * @return The mappings object
     */
    @NotNull
    private Mapping convertEntityMappingInstructionsToMapping(@NotNull final EntityMappingInstructions entityMappingInstructions, @NotNull final List<EntityHeader> forceFailOnNew) {
        final Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setType(entityMappingInstructions.getSourceEntityHeader().getType().toString());
        if(!Goid.DEFAULT_GOID.toString().equals((entityMappingInstructions.getSourceEntityHeader().getStrId()))) {
            mapping.setSrcId(EntityType.ASSERTION_ACCESS.equals(entityMappingInstructions.getSourceEntityHeader().getType()) ? entityMappingInstructions.getSourceEntityHeader().getName() : entityMappingInstructions.getSourceEntityHeader().getStrId());
            final URLAccessible urlAccessible = urlAccessibleLocator.findByEntityType(mapping.getType());
            //only add the url if this item is url accessible
            if(urlAccessible != null) {
                mapping.setSrcUri(urlAccessible.getUrl(entityMappingInstructions.getSourceEntityHeader()));
            }
        }
        mapping.setAction(convertAction(entityMappingInstructions.getMappingAction()));
        if (entityMappingInstructions.shouldFailOnNew() || forceFailOnNew.contains(entityMappingInstructions.getSourceEntityHeader())) {
            mapping.addProperty(FAIL_ON_NEW, Boolean.TRUE);
        }
        if (entityMappingInstructions.shouldFailOnExisting()) {
            mapping.addProperty(FAIL_ON_EXISTING, Boolean.TRUE);
        }
        if (entityMappingInstructions.getTargetMapping() != null) {
            switch (entityMappingInstructions.getTargetMapping().getType()) {
                case GUID:
                    mapping.addProperty(MAP_BY, "guid");
                    break;
                case NAME:
                    mapping.addProperty(MAP_BY, "name");
                    break;
                case ID:
                    if (entityMappingInstructions.getTargetMapping().getTargetID() != null) {
                        //we only need to add map by ID if the target ID is given.
                        mapping.addProperty(MAP_BY, "id");
                    }
                    break;
                case MAP_BY_ROLE_ENTITY:
                    mapping.addProperty(MAP_BY, "mapByRoleEntity");
                    break;
                case MODULE_SHA265:
                    mapping.addProperty(MAP_BY, "moduleSha256");
                    break;
            }
            if (entityMappingInstructions.getTargetMapping().getTargetID() != null) {
                mapping.addProperty(MAP_TO, entityMappingInstructions.getTargetMapping().getTargetID());
            }
        }
        
        for(String propertyNames : entityMappingInstructions.getExtraMappings().stringPropertyNames()) {
            mapping.addProperty(propertyNames, entityMappingInstructions.getExtraMappingProperty(propertyNames));
        }
        return mapping;
    }

    /**
     * This creates a mapping object given the original mapping object and an entity mapping results
     *
     * @param originalMapping     The original mapping object that was used for the bundle import
     * @param entityMappingResult The entity mapping results
     * @return The updated mapping object with the mappings results info specified
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @NotNull
    private Mapping convertMappingAndEntityMappingResultToMapping(@NotNull final Mapping originalMapping, @NotNull final EntityMappingResult entityMappingResult) {
        final Mapping mapping = ManagedObjectFactory.createMapping(originalMapping);

        if (entityMappingResult.isSuccessful()) {
            if (entityMappingResult.getMappingAction() != null) {
                mapping.setActionTaken(convertActionTaken(entityMappingResult.getMappingAction()));
            }
            if (entityMappingResult.getTargetEntityHeader() != null) {
                mapping.setTargetId(EntityType.ASSERTION_ACCESS.equals(entityMappingResult.getTargetEntityHeader().getType()) ? entityMappingResult.getTargetEntityHeader().getName() : entityMappingResult.getTargetEntityHeader().getStrId());
            }
        } else {
            if (entityMappingResult.getException() != null) {
                mapping.setErrorType(getErrorTypeFromException(entityMappingResult.getException()));
                mapping.addProperty("ErrorMessage", ExceptionUtils.getMessage(entityMappingResult.getException()));
            } else {
                throw new IllegalStateException("This should never happen. If a EntityMappingResult is not successful an exception must exist.");
            }
        }
        return mapping;
    }

    /**
     * Creates entity mapping instructions given the mapping and entity.
     *
     * @param mapping         The mapping
     * @param entityContainerMap The entity container map
     * @return The entity mapping instruction for the given mapping and entity
     */
    @NotNull
    private EntityMappingInstructions convertEntityMappingInstructionsFromMappingAndEntity(@NotNull final Mapping mapping, @NotNull final Map<Pair<String, EntityType>, EntityContainer> entityContainerMap) {
        final EntityContainer entityContainer = entityContainerMap.get(new Pair<>(mapping.getSrcId(), EntityType.valueOf(mapping.getType())));
        //Create the source header from the entity
        final EntityHeader sourceHeader;
        if (entityContainer == null) {
            // reference mappings have no referenced entity
            if(EntityType.ASSERTION_ACCESS.name().equals(mapping.getType())) {
                sourceHeader = new EntityHeader((String)null, EntityType.valueOf(mapping.getType()), mapping.getSrcId(), null);
            } else {
                sourceHeader = new EntityHeader(mapping.getSrcId(), EntityType.valueOf(mapping.getType()), null, null);
            }
        } else {
            sourceHeader = EntityHeaderUtils.fromEntity(entityContainer.getEntity());
            if (sourceHeader instanceof AliasHeader) {
                // try to get the alias name from the bundle
                final AliasHeader alias = (AliasHeader) sourceHeader;
                if (alias.getAliasedEntityId() != null) {
                    final EntityContainer aliasedEntity = entityContainerMap.get(new Pair<>(alias.getAliasedEntityId().toString(), alias.getAliasedEntityType()));
                    if (aliasedEntity != null && aliasedEntity.getEntity() instanceof NamedEntity) {
                        final NamedEntity named = (NamedEntity) aliasedEntity.getEntity();
                        alias.setName(named.getName() + " alias");
                    }
                }
            }
        }
        final EntityMappingInstructions.TargetMapping targetMapping;
        if (matchesMapBy(mapping,"name")) {
            targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.NAME, (String) mapping.getProperties().get(MAP_TO));
        } else if (matchesMapBy(mapping, "guid")) {
            targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.GUID, (String) mapping.getProperties().get(MAP_TO));
        } else if (matchesMapBy(mapping, "routingUri")) {
            targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.ROUTING_URI, (String) mapping.getProperties().get(MAP_TO));
        } else if (matchesMapBy(mapping, "path")) {
            targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.PATH, (String) mapping.getProperties().get(MAP_TO));
        } else if (matchesMapBy(mapping, "mapByRoleEntity")) {
            targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.MAP_BY_ROLE_ENTITY, mapping.getTargetId());
        } else if (matchesMapBy(mapping, "moduleSha256")) {
            targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.MODULE_SHA265, mapping.getTargetId());
        } else if (mapping.getTargetId() != null) {
            targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.ID, mapping.getTargetId());
        } else {
            targetMapping = null;
        }

        final Properties extraMappingProperties = new Properties();
        if(mapping.getProperties() != null) {
            for (String key : mapping.getProperties().keySet()) {
                extraMappingProperties.setProperty(key, mapping.getProperties().get(key).toString());
            }
        }

        final Boolean isFailOnExisting = mapping.getProperty(FAIL_ON_EXISTING);
        final Boolean isFailOnNew = mapping.getProperty(FAIL_ON_NEW);
        return new EntityMappingInstructions(sourceHeader, targetMapping, convertMappingAction(mapping.getAction()), isFailOnNew != null ? isFailOnNew : false, isFailOnExisting != null ? isFailOnExisting : false, extraMappingProperties);
    }

    private boolean matchesMapBy(@NotNull final Mapping mappingToCheck, @NotNull final String mapByToMatch) {
        return (mappingToCheck.getProperties() != null && mapByToMatch.equals(mappingToCheck.getProperties().get(MAP_BY)));
    }

    /**
     * Convert an exception to a {@link com.l7tech.gateway.api.Mapping.ErrorType}
     *
     * @param exception The exception to convert
     * @return The error type of the exception
     */
    @NotNull
    private Mapping.ErrorType getErrorTypeFromException(@NotNull final Throwable exception) {
        if (exception instanceof CannotReplaceDependenciesException) {
            return Mapping.ErrorType.CannotReplaceDependency;
        } else if (exception instanceof TargetNotFoundException || exception instanceof ResourceFactory.ResourceNotFoundException || exception instanceof FindException) {
            return Mapping.ErrorType.TargetNotFound;
        } else if (exception instanceof TargetExistsException) {
            return Mapping.ErrorType.TargetExists;
        }  else if (exception instanceof TargetReadOnlyException) {
            return Mapping.ErrorType.TargetReadOnly;
        } else if (exception instanceof IncorrectMappingInstructionsException) {
            return Mapping.ErrorType.ImproperMapping;
        } else if (exception instanceof DuplicateObjectException) {
            return Mapping.ErrorType.UniqueKeyConflict;
        } else if (exception instanceof ConstraintViolationException || exception instanceof ObjectModelException || exception instanceof DataIntegrityViolationException) {
            return Mapping.ErrorType.InvalidResource;
        }
        return Mapping.ErrorType.Unknown;
    }

    /**
     * Gets the equivalent {@link com.l7tech.gateway.api.Mapping.ActionTaken} given a {@link
     * com.l7tech.server.bundling.EntityMappingResult.MappingAction}
     *
     * @param mappingAction The mapping action to convert
     * @return The equivalent action taken
     */
    @NotNull
    private Mapping.ActionTaken convertActionTaken(@NotNull final EntityMappingResult.MappingAction mappingAction) {
        switch (mappingAction) {
            case CreatedNew:
                return Mapping.ActionTaken.CreatedNew;
            case UsedExisting:
                return Mapping.ActionTaken.UsedExisting;
            case UpdatedExisting:
                return Mapping.ActionTaken.UpdatedExisting;
            case Ignored:
                return Mapping.ActionTaken.Ignored;
            case Deleted:
                return Mapping.ActionTaken.Deleted;
            default:
                throw new IllegalArgumentException("Unknown mapping action: " + mappingAction);
        }
    }

    /**
     * Converts between mapping actions
     *
     * @param action The action to convert
     * @return The equivalent action
     */
    @NotNull
    private EntityMappingInstructions.MappingAction convertMappingAction(@NotNull final Mapping.Action action) {
        switch (action) {
            case NewOrExisting:
                return EntityMappingInstructions.MappingAction.NewOrExisting;
            case NewOrUpdate:
                return EntityMappingInstructions.MappingAction.NewOrUpdate;
            case AlwaysCreateNew:
                return EntityMappingInstructions.MappingAction.AlwaysCreateNew;
            case Ignore:
                return EntityMappingInstructions.MappingAction.Ignore;
            case Delete:
                return EntityMappingInstructions.MappingAction.Delete;
            default:
                throw new IllegalArgumentException("Unknown mapping action: " + action);
        }
    }

    /**
     * Converts between mapping actions
     *
     * @param action The action to convert
     * @return The equivalent action
     */
    @NotNull
    private Mapping.Action convertAction(@NotNull final EntityMappingInstructions.MappingAction action) {
        switch (action) {
            case NewOrExisting:
                return Mapping.Action.NewOrExisting;
            case NewOrUpdate:
                return Mapping.Action.NewOrUpdate;
            case AlwaysCreateNew:
                return Mapping.Action.AlwaysCreateNew;
            case Ignore:
                return Mapping.Action.Ignore;
            case Delete:
                return Mapping.Action.Delete;
            default:
                throw new IllegalArgumentException("Unknown mapping action: " + action);
        }
    }
}
