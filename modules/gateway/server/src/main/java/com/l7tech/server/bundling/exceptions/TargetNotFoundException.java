package com.l7tech.server.bundling.exceptions;

import com.l7tech.server.bundling.EntityMappingInstructions;
import org.jetbrains.annotations.NotNull;

/**
 * This is thrown if an entity was expected on the target gateway but could not be found
 */
public class TargetNotFoundException extends BundleImportException {
    private static final long serialVersionUID = -213635002473924005L;

    private static final String exceptionMessage = "Could not locate entity: %1$s. Source Entity: %2$s";

    /**
     * Create a new target not found exception.
     *
     * @param mapping The mapping instructions that are incorrect
     * @param message The error message
     */
    public TargetNotFoundException(@NotNull final EntityMappingInstructions mapping, @NotNull final String message) {
        super(mapping, String.format(exceptionMessage, message, mapping.getSourceEntityHeader().toStringVerbose()));
    }
}
