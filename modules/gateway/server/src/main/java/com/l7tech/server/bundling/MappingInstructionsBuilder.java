package com.l7tech.server.bundling;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * This is the default mapping instructions builder. It is used to build default mapping instructions by the entity
 * bundle exporter. For modular assertions a custom default mapping instructions builder can be added by calling the
 * {@link #registerMappingInstructionBuilder(com.l7tech.objectmodel.EntityType, com.l7tech.util.Functions.Ternary)}
 * method.
 * <p/>
 * If there isn't a custom builder registered the default is to build a mapping with fail on new and fail on existing
 * both false. The given mapping action and type will be used.
 */
public class MappingInstructionsBuilder {

    //The map of registered mapping instructions
    @NotNull
    private final Map<EntityType, Functions.Ternary<EntityMappingInstructions, EntityHeader, EntityMappingInstructions.MappingAction, EntityMappingInstructions.TargetMapping.Type>> mappingInstructionBuilders;

    // The default mapping instructions builder to use if there isnt a custom one registered.
    private static final Functions.Ternary<EntityMappingInstructions, EntityHeader, EntityMappingInstructions.MappingAction, EntityMappingInstructions.TargetMapping.Type> defaultMappingInstructionsBuilder = new Functions.Ternary<EntityMappingInstructions, EntityHeader, EntityMappingInstructions.MappingAction, EntityMappingInstructions.TargetMapping.Type>() {
        @Override
        public EntityMappingInstructions call(EntityHeader sourceEntityHeader, EntityMappingInstructions.MappingAction mappingAction, EntityMappingInstructions.TargetMapping.Type mappingType) {
            return new EntityMappingInstructions(sourceEntityHeader, EntityMappingInstructions.TargetMapping.Type.ID.equals(mappingType) ? null : new EntityMappingInstructions.TargetMapping(mappingType), mappingAction);
        }
    };

    /**
     * Creates a MappingInstructionsBuilder the default builders for core entities are added here.
     */
    public MappingInstructionsBuilder() {
        //Create any default builders
        mappingInstructionBuilders = CollectionUtils.MapBuilder.<EntityType, Functions.Ternary<EntityMappingInstructions, EntityHeader, EntityMappingInstructions.MappingAction, EntityMappingInstructions.TargetMapping.Type>>builder()
                //For identity providers fail if one doesn't already exist.
                .put(EntityType.ID_PROVIDER_CONFIG, new Functions.Ternary<EntityMappingInstructions, EntityHeader, EntityMappingInstructions.MappingAction, EntityMappingInstructions.TargetMapping.Type>() {
                    @Override
                    public EntityMappingInstructions call(EntityHeader sourceEntityHeader, EntityMappingInstructions.MappingAction mappingAction, EntityMappingInstructions.TargetMapping.Type mappingType) {
                        return new EntityMappingInstructions(sourceEntityHeader, EntityMappingInstructions.TargetMapping.Type.ID.equals(mappingType) ? null : new EntityMappingInstructions.TargetMapping(mappingType), mappingAction, true, false);
                    }
                })
                .map();
    }

    /**
     * This will create a default {@link com.l7tech.server.bundling.EntityMappingInstructions} to use for the given
     * entity header. It is also given the default mapping action and the default mapping type
     *
     * @param sourceEntityHeader The Entity header to create the default mapping instructions for
     * @param mappingAction      The default mapping action to use
     * @param mappingType        The default mapping type to use.
     * @return The EntityMappingInstructions for the given entity header.
     */
    public EntityMappingInstructions createDefaultMapping(EntityHeader sourceEntityHeader, EntityMappingInstructions.MappingAction mappingAction, EntityMappingInstructions.TargetMapping.Type mappingType) {
        //find if there is a custom mapping instructions builder registered
        Functions.Ternary<EntityMappingInstructions, EntityHeader, EntityMappingInstructions.MappingAction, EntityMappingInstructions.TargetMapping.Type> mappingInstructionBuilder = mappingInstructionBuilders.get(sourceEntityHeader.getType());
        if (mappingInstructionBuilder != null) {
            //if there is a custom builder registered call it.
            return mappingInstructionBuilder.call(sourceEntityHeader, mappingAction, mappingType);
        } else {
            //there is no custom builder registered so use the default
            return defaultMappingInstructionsBuilder.call(sourceEntityHeader, mappingAction, mappingType);
        }
    }

    /**
     * Register a new mapping instructions builder. This will fail with an {@link java.lang.IllegalStateException} if
     * there an instructions build already registered for the given entity type
     *
     * @param entityType                The entity type to add a custom mapping instructions builder for
     * @param mappingInstructionBuilder The Custom mapping instruction builder. It is a Ternary function where the first
     *                                  argument is the entity header of the entity to build the mapping for, the second
     *                                  argument is the default mapping action to use, and the third argument is the
     *                                  default mapping target type to use
     * @throws java.lang.IllegalStateException This is thrown if there is already a mapping instructions builder
     *                                         registered for the given EntityType
     */
    public void registerMappingInstructionBuilder(EntityType entityType, Functions.Ternary<EntityMappingInstructions, EntityHeader, EntityMappingInstructions.MappingAction, EntityMappingInstructions.TargetMapping.Type> mappingInstructionBuilder) {
        if (mappingInstructionBuilders.containsKey(entityType)) {
            throw new IllegalStateException("Cannot add a mapping instructions builder for " + entityType + " there is one already registered.");
        }
        mappingInstructionBuilders.put(entityType, mappingInstructionBuilder);
    }
}
