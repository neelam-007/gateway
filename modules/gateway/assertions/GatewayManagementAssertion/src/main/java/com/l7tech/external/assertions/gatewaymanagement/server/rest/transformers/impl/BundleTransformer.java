package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.APIUtilityLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.URLAccessibleLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.bundling.EntityBundle;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.bundling.EntityMappingInstructions;
import com.l7tech.server.bundling.EntityMappingResult;
import com.l7tech.server.bundling.exceptions.IncorrectMappingInstructionsException;
import com.l7tech.server.bundling.exceptions.TargetExistsException;
import com.l7tech.server.bundling.exceptions.TargetNotFoundException;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This a the bundle transformer. It will transform a bundle to an internal EntityBundle and back. It is also used to
 * transform mappings
 */
@SuppressWarnings("unchecked")
@Component
public class BundleTransformer implements APITransformer<Bundle, EntityBundle> {

    @Inject
    private APIUtilityLocator apiUtilityLocator;
    @Inject
    private URLAccessibleLocator urlAccessibleLocator;

    //Theses are the different properties that can be in a Mapping
    private static final String FailOnNew = "FailOnNew";
    private static final String FailOnExisting = "FailOnExisting";
    private static final String MapBy = "MapBy";
    private static final String MapTo = "MapTo";

    @NotNull
    @Override
    public String getResourceType() {
        return "BUNDLE";
    }

    /**
     * Converts a Entity bundle into a Bundle
     *
     * @param entityBundle The entity bundle to convert to a Bundle
     * @return The Bundle
     */
    @NotNull
    @Override
    public Bundle convertToMO(@NotNull final EntityBundle entityBundle) {
        final ArrayList<Item> items = new ArrayList<>();
        final ArrayList<Mapping> mappings = new ArrayList<>();

        for (final EntityMappingInstructions entityMappingInstruction : entityBundle.getMappingInstructions()) {
            final EntityContainer entityResource = entityBundle.getEntity(entityMappingInstruction.getSourceEntityHeader().getStrId(), entityMappingInstruction.getSourceEntityHeader().getType());
            if (entityResource != null) {
                //Get the transformer for the entity so that it can be converted.
                final EntityAPITransformer transformer = apiUtilityLocator.findTransformerByResourceType(entityMappingInstruction.getSourceEntityHeader().getType().toString());
                if (transformer == null) {
                    throw new IllegalStateException("Cannot locate a transformer for entity type: " + entityMappingInstruction.getSourceEntityHeader().getType());
                }
                //get the MO from the entity
                final Object mo = transformer.convertToMO(entityResource.getEntity());
                //create an item from the mo
                final Item<?> item = transformer.convertToItem(mo);
                //add the item to the bundle items list
                items.add(item);
            }

            //convert the mapping
            mappings.add(convertEntityMappingInstructionsToMapping(entityMappingInstruction));
        }

        final Bundle bundle = ManagedObjectFactory.createBundle();
        bundle.setReferences(items);
        bundle.setMappings(mappings);
        return bundle;
    }

    /**
     * This converts a bundle to an entity bundle.
     *
     * @param bundle The bundle to convert
     * @return The entity bundle created from the given bundle
     * @throws ResourceFactory.InvalidResourceException
     */
    @Override
    @NotNull
    public EntityBundle convertFromMO(@NotNull final Bundle bundle) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(bundle, true);
    }

    /**
     * This converts a bundle to an entity bundle.
     *
     * @param bundle The bundle to convert
     * @param strict This have no effect for bundles
     * @return The entity bundle created from the given bundle
     * @throws ResourceFactory.InvalidResourceException
     */
    @Override
    @NotNull
    public EntityBundle convertFromMO(@NotNull final Bundle bundle, final boolean strict) throws ResourceFactory.InvalidResourceException {
        // Convert all the MO's in the bundle to entities

        final List<EntityContainer> entityContainers = Functions.map(bundle.getReferences(), new Functions.UnaryThrows<EntityContainer, Item, ResourceFactory.InvalidResourceException>() {
            @Override
            public EntityContainer call(Item item) throws ResourceFactory.InvalidResourceException {
                final EntityAPITransformer transformer = apiUtilityLocator.findTransformerByResourceType(item.getType());
                if (transformer == null) {
                    throw new IllegalStateException("Cannot locate a transformer for entity type: " + item.getType());
                }
                //cannot be strict here because there will be many cases where the reference entities in the mo's do not exist on the gateway
                return (EntityContainer) transformer.convertFromMO(item.getContent(), false);
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
        if (entityContainerMap.size() != entityContainers.size()) {
            throw new IllegalStateException("Could not uniquely map EntityContainers by their id's.");
        }

        //convert the mappings to entityMapping instruction.
        final List<EntityMappingInstructions> mappingInstructions = Functions.map(bundle.getMappings(), new Functions.UnaryThrows<EntityMappingInstructions, Mapping, ResourceFactory.InvalidResourceException>() {
            @Override
            public EntityMappingInstructions call(Mapping mapping) throws ResourceFactory.InvalidResourceException {
                return convertEntityMappingInstructionsFromMappingAndEntity(mapping, entityContainerMap.get(new Pair<>(mapping.getSrcId(), EntityType.valueOf(mapping.getType()))));
            }
        });

        return new EntityBundle(entityContainers, mappingInstructions);
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
            final Mapping mapping = mappingsMap.get(new Pair<>(entityMappingResult.getSourceEntityHeader().getStrId(), entityMappingResult.getSourceEntityHeader().getType()));
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
     * @return The mappings object
     */
    @NotNull
    private Mapping convertEntityMappingInstructionsToMapping(@NotNull final EntityMappingInstructions entityMappingInstructions) {
        final Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setType(entityMappingInstructions.getSourceEntityHeader().getType().toString());
        mapping.setSrcId(entityMappingInstructions.getSourceEntityHeader().getStrId());
        final URLAccessible urlAccessible = urlAccessibleLocator.findByEntityType(mapping.getType());
        mapping.setSrcUri(urlAccessible.getUrl(entityMappingInstructions.getSourceEntityHeader()));
        mapping.setAction(convertAction(entityMappingInstructions.getMappingAction()));
        if (entityMappingInstructions.shouldFailOnNew()) {
            mapping.addProperty(FailOnNew, Boolean.TRUE);
        }
        if (entityMappingInstructions.shouldFailOnExisting()) {
            mapping.addProperty(FailOnExisting, Boolean.TRUE);
        }
        if (entityMappingInstructions.getTargetMapping() != null) {
            switch (entityMappingInstructions.getTargetMapping().getType()) {
                case GUID:
                    mapping.addProperty(MapBy, "guid");
                    break;
                case NAME:
                    mapping.addProperty(MapBy, "name");
                    break;
                case ID:
                    if (entityMappingInstructions.getTargetMapping().getTargetID() != null) {
                        //we only need to add map by ID if the target ID is given.
                        mapping.addProperty(MapBy, "id");
                    }
                    break;
            }
            if (entityMappingInstructions.getTargetMapping().getTargetID() != null) {
                mapping.addProperty(MapTo, entityMappingInstructions.getTargetMapping().getTargetID());
            }
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
                mapping.setTargetId(entityMappingResult.getTargetEntityHeader().getStrId());
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
     * @param entityContainer The entity container
     * @return The entity mapping instruction for the given mapping and entity
     */
    @NotNull
    private EntityMappingInstructions convertEntityMappingInstructionsFromMappingAndEntity(@NotNull final Mapping mapping, @Nullable final EntityContainer entityContainer) {
        //Create the source header from the entity
        final EntityHeader sourceHeader;
        if (entityContainer == null) {
            // reference mappings have no referenced entity
            sourceHeader = new EntityHeader(mapping.getSrcId(), EntityType.valueOf(mapping.getType()), null, null);
        } else {
            sourceHeader = EntityHeaderUtils.fromEntity(entityContainer.getEntity());
        }
        final EntityMappingInstructions.TargetMapping targetMapping;
        if (mapping.getProperties() != null && "name".equals(mapping.getProperties().get(MapBy))) {
            targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.NAME, (String) mapping.getProperties().get(MapTo));
        } else if (mapping.getProperties() != null && "guid".equals(mapping.getProperties().get(MapBy))) {
            targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.GUID, (String) mapping.getProperties().get(MapTo));
        } else if (mapping.getTargetId() != null) {
            targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.ID, mapping.getTargetId());
        } else {
            targetMapping = null;
        }
        final Boolean isFailOnExisting = mapping.getProperty(FailOnExisting);
        final Boolean isFailOnNew = mapping.getProperty(FailOnNew);
        return new EntityMappingInstructions(sourceHeader, targetMapping, convertMappingAction(mapping.getAction()), isFailOnNew != null ? isFailOnNew : false, isFailOnExisting != null ? isFailOnExisting : false);
    }

    /**
     * Convert an exception to a {@link com.l7tech.gateway.api.Mapping.ErrorType}
     *
     * @param exception The exception to convert
     * @return The error type of the exception
     */
    @NotNull
    private Mapping.ErrorType getErrorTypeFromException(@NotNull final Throwable exception) {
        //TODO: improve exception handling
        if (exception instanceof CannotReplaceDependenciesException) {
            return Mapping.ErrorType.CannotReplaceDependency;
        } else if (exception instanceof TargetNotFoundException || exception instanceof ResourceFactory.ResourceNotFoundException || exception instanceof FindException) {
            return Mapping.ErrorType.TargetNotFound;
        } else if (exception instanceof TargetExistsException) {
            return Mapping.ErrorType.TargetExists;
        } else if (exception instanceof IncorrectMappingInstructionsException) {
            return Mapping.ErrorType.ImproperMapping;
        } else if (exception instanceof DuplicateObjectException) {
            return Mapping.ErrorType.UniqueKeyConflict;
        } else if (exception instanceof ConstraintViolationException || exception instanceof ObjectModelException) {
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
            default:
                throw new IllegalArgumentException("Unknown mapping action: " + action);
        }
    }
}
