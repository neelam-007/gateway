package com.l7tech.server.bundling;

import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is a mapping instruction to tell the {@link com.l7tech.server.bundling.EntityBundleImporter} how to properly
 * import an entity
 */
public class EntityMappingInstructions {
    /**
     * The source entity header
     */
    @NotNull
    private final EntityHeader sourceEntityHeader;
    /**
     * Defaults to {@link com.l7tech.server.bundling.EntityMappingInstructions.MappingAction#NewOrExisting}
     */
    @NotNull
    private final MappingAction mappingAction;
    /**
     * defaults to false
     */
    private final boolean failOnNew;
    /**
     * defaults to false
     */
    private final boolean failOnExisting;
    /**
     * the target mapping describe how to map
     */
    @Nullable
    private final TargetMapping targetMapping;

    public enum MappingAction {
        NewOrUpdate, NewOrExisting, AlwaysCreateNew, Ignore, Delete
    }

    /**
     * Creates a mapping. This is equivalent to calling EntityMappingInstructions(EntityHeader, null)
     *
     * @param sourceEntityHeader The source entity header. This must have an id specified and reference an entity with
     *                           the same id in an {@link com.l7tech.server.bundling.EntityBundle}. Must not be null
     * @see #EntityMappingInstructions(com.l7tech.objectmodel.EntityHeader, com.l7tech.server.bundling.EntityMappingInstructions.TargetMapping,
     * com.l7tech.server.bundling.EntityMappingInstructions.MappingAction, boolean, boolean)
     */
    public EntityMappingInstructions(@NotNull final EntityHeader sourceEntityHeader) {
        this(sourceEntityHeader, null);
    }

    /**
     * Creates a mapping. This is equivalent to calling EntityMappingInstructions(EntityHeader, null,
     * MappingAction.NewOrExisting)
     *
     * @param sourceEntityHeader The source entity header. This must have an id specified and reference an entity with
     *                           the same id in an {@link com.l7tech.server.bundling.EntityBundle}. Must not be null
     * @param targetMapping      The target mapping. If this is null mapping will be done using the entity id. If it is
     *                           not null mapping will be done using the {@link com.l7tech.server.bundling.EntityMappingInstructions.TargetMapping}
     *                           type and id.
     * @see #EntityMappingInstructions(com.l7tech.objectmodel.EntityHeader, com.l7tech.server.bundling.EntityMappingInstructions.TargetMapping,
     * com.l7tech.server.bundling.EntityMappingInstructions.MappingAction, boolean, boolean)
     */
    public EntityMappingInstructions(@NotNull final EntityHeader sourceEntityHeader, @Nullable final TargetMapping targetMapping) {
        this(sourceEntityHeader, targetMapping, MappingAction.NewOrExisting);
    }

    /**
     * Creates a mapping. This is equivalent to calling EntityMappingInstructions(EntityHeader, null,
     * MappingAction.NewOrExisting, false, false)
     *
     * @param sourceEntityHeader The source entity header. This must have an id specified and reference an entity with
     *                           the same id in an {@link com.l7tech.server.bundling.EntityBundle}. Must not be null
     * @param targetMapping      The target mapping. If this is null mapping will be done using the entity id. If it is
     *                           not null mapping will be done using the {@link com.l7tech.server.bundling.EntityMappingInstructions.TargetMapping}
     *                           type and id.
     * @param mappingAction      The mapping action to take. This tells the {@link com.l7tech.server.bundling.EntityBundleImporter}
     *                           how to behave if there is an existing entity in the gateway.
     * @see #EntityMappingInstructions(com.l7tech.objectmodel.EntityHeader, com.l7tech.server.bundling.EntityMappingInstructions.TargetMapping,
     * com.l7tech.server.bundling.EntityMappingInstructions.MappingAction, boolean, boolean)
     */
    public EntityMappingInstructions(@NotNull final EntityHeader sourceEntityHeader, @Nullable final TargetMapping targetMapping, @NotNull final MappingAction mappingAction) {
        this(sourceEntityHeader, targetMapping, mappingAction, false, false);
    }

    /**
     * Creates new mapping instructions.
     *
     * @param sourceEntityHeader The source entity header. This must have an id specified and reference an entity with
     *                           the same id in an {@link com.l7tech.server.bundling.EntityBundle}. Must not be null
     * @param targetMapping      The target mapping. If this is null mapping will be done using the entity id. If it is
     *                           not null mapping will be done using the {@link com.l7tech.server.bundling.EntityMappingInstructions.TargetMapping}
     *                           type and id.
     * @param mappingAction      The mapping action to take. This tells the {@link com.l7tech.server.bundling.EntityBundleImporter}
     *                           how to behave if there is an existing entity in the gateway.
     * @param failOnNew          If true this will fail the import if there isn't an existing entity to map the source
     *                           entity to.
     * @param failOnExisting     If this is true the import will fail if there is an existing entity.
     */
    public EntityMappingInstructions(@NotNull final EntityHeader sourceEntityHeader, @Nullable final TargetMapping targetMapping, @NotNull final MappingAction mappingAction, final boolean failOnNew, final boolean failOnExisting) {
        this.sourceEntityHeader = sourceEntityHeader;
        this.targetMapping = targetMapping;
        this.mappingAction = mappingAction;
        this.failOnNew = failOnNew;
        this.failOnExisting = failOnExisting;
    }

    /**
     * The source entity header. This will map to an entity in the {@link com.l7tech.server.bundling.EntityBundle}
     *
     * @return The source entity header
     */
    @NotNull
    public EntityHeader getSourceEntityHeader() {
        return sourceEntityHeader;
    }

    /**
     * Gets the mapping action. This tells the {@link com.l7tech.server.bundling.EntityBundleImporter} how to map the
     * entity.
     *
     * @return The mapping action. Defaults to MappingAction.NewOrExisting
     */
    @NotNull
    public MappingAction getMappingAction() {
        return mappingAction;
    }

    /**
     * Returns true if it should fail if there is no existing entity to map this one to
     *
     * @return true if it should fail if there is no existing entity to map this one to. defaults to false
     */
    public boolean shouldFailOnNew() {
        return failOnNew;
    }

    /**
     * Returns true if it should fail if there is an existing entity to map this one to
     *
     * @return true if it should fail if there is an existing entity to map this one to. defaults to false
     */
    public boolean shouldFailOnExisting() {
        return failOnExisting;
    }

    @Override
    public int hashCode() {
        return sourceEntityHeader.hashCode();
    }

    /**
     * Is the same mapping if it maps the same resource
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityMappingInstructions instruction = (EntityMappingInstructions)o;

        if (!sourceEntityHeader.equals(instruction.sourceEntityHeader)) return false;

        return true;
    }

    /**
     * The targetMapping that identifies how to map this entity. Defaults to null. If it is null the entity id will be
     * used to map the entity. If it is not null the Target mapping type and id will be used to map the entity
     *
     * @return The target entity header to map to.
     */
    @Nullable
    public TargetMapping getTargetMapping() {
        return targetMapping;
    }

    /**
     * This represents the target mapping for an entity.
     */
    public static class TargetMapping {
        //The ID to map the entity to
        @Nullable
        private final String targetID;
        //The type of the target id
        @NotNull
        private final Type type;

        public enum Type {
            ID, NAME, GUID, MAP_BY_ROLE_ENTITY
        }

        /**
         * Creates a new target mapping with the given type and a null id
         *
         * @param type The type of the mapping
         */
        public TargetMapping(@NotNull final Type type) {
            this(type, null);
        }

        /**
         * Creates a new target mapping with the given type and target id.
         *
         * @param type     The type of the mapping
         * @param targetID The target id
         */
        public TargetMapping(@NotNull final Type type, @Nullable final String targetID) {
            this.targetID = targetID;
            this.type = type;
        }

        /**
         * Returns the target id for this mapping. If it is null it is expected that the id will come from the source
         * entity.
         *
         * @return The target id
         */
        @Nullable
        public String getTargetID() {
            return targetID;
        }

        /**
         * This is the type of the target mapping. It specifies the type of the target id.
         *
         * @return The target type mapping
         */
        @NotNull
        public Type getType() {
            return type;
        }
    }
}
