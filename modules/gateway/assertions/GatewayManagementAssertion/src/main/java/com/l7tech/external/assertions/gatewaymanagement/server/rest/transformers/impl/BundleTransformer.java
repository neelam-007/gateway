package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.APIUtilityLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.URLAccessibleLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.bundling.*;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;

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
    private static final String IdentityProvider = "IdentityProvider";
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
    @Override
    public Bundle convertToMO(@NotNull EntityBundle entityBundle) {
        final ArrayList<Item> items = new ArrayList<>();
        final ArrayList<Mapping> mappings = new ArrayList<>();

        for (final EntityMappingInstructions entityMappingInstruction : entityBundle.getMappingInstructions()) {
            EntityContainer entityResource = entityBundle.getEntity(entityMappingInstruction.getSourceEntityHeader().getStrId());
            if( entityResource != null){
                //Get the transformer for the entity so that it can be converted.
                APITransformer transformer = apiUtilityLocator.findTransformerByResourceType(entityMappingInstruction.getSourceEntityHeader().getType().toString());

                //get the MO from the entity
                Object mo = transformer.convertToMO(entityResource.getEntity());
                //create an item from the mo
                Item<?> item = transformer.convertToItem(mo);
                //add the item to the bundle items list
                items.add(item);
            }

            //convert the mapping
            mappings.add(convertEntityMappingInstructionsToMapping(entityMappingInstruction));
        }

        Bundle bundle = ManagedObjectFactory.createBundle();
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
    public EntityContainer<EntityBundle> convertFromMO(@NotNull Bundle bundle) throws ResourceFactory.InvalidResourceException {
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
    public EntityContainer<EntityBundle> convertFromMO(@NotNull Bundle bundle, boolean strict)  throws ResourceFactory.InvalidResourceException {
         // Convert all the MO's in the bundle to entities

        final List<EntityContainer> entityContainers = Functions.map(bundle.getReferences(), new Functions.UnaryThrows<EntityContainer, Item, ResourceFactory.InvalidResourceException>() {
            @Override
            public EntityContainer call(Item item) throws ResourceFactory.InvalidResourceException {
                APITransformer transformer = apiUtilityLocator.findTransformerByResourceType(item.getType());
                //cannot be strict here because there will be many cases where the reference entities in the mo's do not exist on the gateway
                return transformer.convertFromMO(item.getContent(), false);
            }
        });

        //Create a map of the entities so that they can quickly be referenced.
        final Map<String, EntityContainer> entityContainerMap = Functions.toMap(entityContainers, new Functions.Unary<Pair<String, EntityContainer>, EntityContainer>() {
            @Override
            public Pair<String, EntityContainer> call(EntityContainer item) {
                return new Pair<>(item.getId(), item);
            }
        });

        //convert the mappings to entityMapping instruction.
        final List<EntityMappingInstructions> mappingInstructions = Functions.map(bundle.getMappings(), new Functions.UnaryThrows<EntityMappingInstructions, Mapping, ResourceFactory.InvalidResourceException>() {
            @Override
            public EntityMappingInstructions call(Mapping mapping) throws ResourceFactory.InvalidResourceException {
                return convertEntityMappingInstructionsFromMappingAndEntity(mapping, entityContainerMap.get(mapping.getSrcId()));
            }
        });

        return new EntityContainer<EntityBundle>(new EntityBundle(entityContainers, mappingInstructions));
    }

    /**
     * This is not supported. A bundle can not be represented as a header
     */
    @Override
    public EntityHeader convertToHeader(Bundle bundle) throws ResourceFactory.InvalidResourceException {
        throw new UnsupportedOperationException("Cannot convert a bundle to a header.");
    }

    @Override
    @NotNull
    public Item<Bundle> convertToItem(@NotNull Bundle bundle) {
        return new ItemBuilder<Bundle>("Bundle", "BUNDLE")
                .setContent(bundle)
                .build();
    }

    /**
     * This is not supported. A bundle can not be represented as a header
     */
    @Override
    public Item<Bundle> convertToItem(EntityHeader header) {
        throw new UnsupportedOperationException("Cannot convert a bundle to an item given a header.");
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
    public List<Mapping> updateMappings(@NotNull List<Mapping> mappings, @NotNull List<EntityMappingResult> mappingsPerformed) {
        //create a mappings map so that they can be easily retrieved.
        final Map<String, Mapping> mappingsMap = Functions.toMap(mappings, new Functions.Unary<Pair<String, Mapping>, Mapping>() {
            @Override
            public Pair<String, Mapping> call(Mapping mapping) {
                return new Pair<>(mapping.getSrcId(), mapping);
            }
        });

        final List<Mapping> updatedMappings = new ArrayList<>();
        for(EntityMappingResult entityMappingResult : mappingsPerformed) {
            //get the mappings for this EntityMappingResult
            Mapping mapping = mappingsMap.get(entityMappingResult.getSourceEntityHeader().getStrId());
            //get the updated mapping
            final Mapping mappingUpdated = convertMappingAndEntityMappingResultToMapping(mapping, entityMappingResult);
            //set the target url
            if (entityMappingResult.getTargetEntityHeader() != null) {
                URLAccessible urlAccessible = urlAccessibleLocator.findByEntityType(mapping.getType());
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
    private Mapping convertEntityMappingInstructionsToMapping(@NotNull EntityMappingInstructions entityMappingInstructions) {
        final Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setType(entityMappingInstructions.getSourceEntityHeader().getType().toString());
        mapping.setSrcId(entityMappingInstructions.getSourceEntityHeader().getStrId());
        mapping.setAction(convertAction(entityMappingInstructions.getMappingAction()));
        if (entityMappingInstructions.shouldFailOnNew()) {
            mapping.addProperty(FailOnNew, Boolean.TRUE);
        }
        if (entityMappingInstructions.shouldFailOnExisting()) {
            mapping.addProperty(FailOnExisting, Boolean.TRUE);
        }
        if(entityMappingInstructions.getIdentityProviderId() != null ){
            mapping.addProperty(IdentityProvider, entityMappingInstructions.getIdentityProviderId().toString());
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
    private Mapping convertMappingAndEntityMappingResultToMapping(@NotNull Mapping originalMapping, @NotNull EntityMappingResult entityMappingResult) {
        final Mapping mapping = ManagedObjectFactory.createMapping(originalMapping);

        if (entityMappingResult.isSuccessful()) {
            if(entityMappingResult.getMappingAction() != null) {
                mapping.setActionTaken(convertActionTaken(entityMappingResult.getMappingAction()));
            }
            if (entityMappingResult.getTargetEntityHeader() != null) {
                mapping.setTargetId(entityMappingResult.getTargetEntityHeader().getStrId());
            }
        } else if (!entityMappingResult.isSuccessful() && entityMappingResult.getException() != null) {
            mapping.setErrorType(getErrorTypeFromException(entityMappingResult.getException()));
            mapping.addProperty("ErrorMessage", entityMappingResult.getException().getMessage());
        }
        return mapping;
    }

    /**
     * Creates entity mapping instructions given the mapping and entity.
     *
     * @param mapping The mapping
     * @param entity  The entity
     * @return The entity mapping instruction for the given mapping and entity
     */
    @NotNull
    private EntityMappingInstructions convertEntityMappingInstructionsFromMappingAndEntity(@NotNull Mapping mapping, EntityContainer entity) {
        //Create the source header from the entity
        EntityHeader sourceHeader;
        if(entity == null ) {
            if(mapping.getProperties().containsKey(IdentityProvider)){
                sourceHeader = new IdentityHeader(Goid.parseGoid((String)mapping.getProperties().get(IdentityProvider)),mapping.getSrcId(),EntityType.valueOf(mapping.getType()),null,null,null,null);

            }else{
                // reference mappings have no referenced entity
                sourceHeader = new EntityHeader(mapping.getSrcId(),EntityType.valueOf(mapping.getType()),null,null);
            }
        }
        else {
            sourceHeader = EntityHeaderUtils.fromEntity((Entity)entity.getEntity());
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
        Boolean isFailOnExisting = mapping.getProperty(FailOnExisting);
        Boolean isFailOnNew = mapping.getProperty(FailOnNew);
        String idProviderStr = mapping.getProperty(IdentityProvider);
        Goid idProvider = idProviderStr == null ? null : Goid.parseGoid(idProviderStr);
        return new EntityMappingInstructions(sourceHeader, targetMapping, convertMappingAction(mapping.getAction()), isFailOnNew != null ? isFailOnNew : false, isFailOnExisting != null ? isFailOnExisting : false, idProvider);
    }

    /**
     * Convert an exception to a {@link com.l7tech.gateway.api.Mapping.ErrorType}
     *
     * @param exception The exception to convert
     * @return The error type of the exception
     */
    @NotNull
    private Mapping.ErrorType getErrorTypeFromException(@NotNull Throwable exception) {
        if (exception instanceof CannotReplaceDependenciesException) {
            return Mapping.ErrorType.CannotReplaceDependency;
        } else if (exception instanceof CannotRetrieveDependenciesException) {
            return Mapping.ErrorType.TargetNotFound;
        } else if (exception instanceof ResourceFactory.ResourceNotFoundException || exception instanceof FindException) {
            return Mapping.ErrorType.TargetNotFound;
        }
        //TODO: This should not be the default exception!
        return Mapping.ErrorType.UniqueKeyConflict;
    }

    /**
     * Gets the equivalent {@link com.l7tech.gateway.api.Mapping.ActionTaken} given a {@link
     * com.l7tech.server.bundling.EntityMappingResult.MappingAction}
     *
     * @param mappingAction The mapping action to convert
     * @return The equivalent action taken
     */
    @NotNull
    private Mapping.ActionTaken convertActionTaken(@NotNull EntityMappingResult.MappingAction mappingAction) {
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
    private EntityMappingInstructions.MappingAction convertMappingAction(@NotNull Mapping.Action action) {
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
    private Mapping.Action convertAction(@NotNull EntityMappingInstructions.MappingAction action) {
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
