package com.l7tech.server.bundling;

import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This represents the mapping result for an entity following bundle import using the {@link
 * com.l7tech.server.bundling.EntityBundleImporter}.
 */
public class EntityMappingResult {
    @NotNull
    private final EntityHeader sourceEntityHeader;
    @Nullable
    private final MappingAction mappingAction;
    @Nullable
    private final EntityHeader targetEntityHeader;
    @Nullable
    private final Throwable exception;

    /**
     * This is a mapping action that was taken on an entity during import
     */
    public enum MappingAction {
        CreatedNew, UsedExisting, UpdatedExisting, Ignored
    }

    /**
     * Creates a new EntityMappingResult. This represents a successful mapping.
     *
     * @param sourceEntityHeader The source entity header that was imported
     * @param targetEntityHeader The target entity header that it was mapped to or that was created.
     * @param mappingAction      The mapping action that was taken.
     */
    @SuppressWarnings("NullableProblems")
    public EntityMappingResult(@NotNull final EntityHeader sourceEntityHeader, @NotNull final EntityHeader targetEntityHeader, @NotNull final MappingAction mappingAction) {
        this.sourceEntityHeader = sourceEntityHeader;
        this.mappingAction = mappingAction;
        this.targetEntityHeader = targetEntityHeader;
        this.exception = null;
    }

    /**
     * Creates a new EntityMappingResult. This represents an unsuccessful mapping.
     *
     * @param sourceEntityHeader The source entity header that was attempted to be imported.
     * @param exception          The throwable exception that occurred attempting to import the source entity
     */
    @SuppressWarnings("NullableProblems")
    public EntityMappingResult(@NotNull final EntityHeader sourceEntityHeader, @NotNull final Throwable exception) {
        this.sourceEntityHeader = sourceEntityHeader;
        this.exception = exception;
        this.mappingAction = null;
        this.targetEntityHeader = null;
    }

    /**
     * Creates a new EntityMappingResult. This represents an Ignored mapping result. The target entity header is null
     * and the mapping action is {@link com.l7tech.server.bundling.EntityMappingResult.MappingAction#Ignored}
     *
     * @param sourceEntityHeader The source entity header that was ignored in the import.
     */
    public EntityMappingResult(@NotNull final EntityHeader sourceEntityHeader) {
        this.sourceEntityHeader = sourceEntityHeader;
        this.exception = null;
        this.mappingAction = MappingAction.Ignored;
        this.targetEntityHeader = null;
    }

    /**
     * Returns the source entity header that this mapping result is for.
     *
     * @return The source entity header that this mapping is for.
     */
    @NotNull
    public EntityHeader getSourceEntityHeader() {
        return sourceEntityHeader;
    }

    /**
     * The mapping action that occurred during the import. This will be null if there was an exception during the
     * import
     *
     * @return The mapping action that was taken during the import
     */
    @Nullable
    public MappingAction getMappingAction() {
        return mappingAction;
    }

    /**
     * The target entity header that the entity was imported to.
     *
     * @return the target entity header that the entity was mapping to. This will be null if there was an exception
     * during the import.
     */
    @Nullable
    public EntityHeader getTargetEntityHeader() {
        return targetEntityHeader;
    }

    /**
     * This is an exception that that occurred during the import of this entity. This is null if the entity was
     * successfully imported.
     *
     * @return The throwable exception that occurred during the import of the entity. This is null if the import was
     * successful.
     */
    @Nullable
    public Throwable getException() {
        return exception;
    }

    public boolean isSuccessful() {
        return exception == null;
    }
}
