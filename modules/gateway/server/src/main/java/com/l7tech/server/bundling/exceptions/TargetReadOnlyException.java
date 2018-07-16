package com.l7tech.server.bundling.exceptions;

import com.l7tech.server.bundling.EntityMappingInstructions;
import org.jetbrains.annotations.NotNull;

/**
 * This is thrown if a target entity is read-only.
 */
public class TargetReadOnlyException extends BundleImportException {
    private static final long serialVersionUID = 1691021891251179032L;

    private static final String exceptionMessage = "Target entity is read-only: %1$s. Source Entity: %2$s";

    /**
     * Create a new target read-only mapping exception.
     *
     * @param mapping    The mapping instructions that map to the existing
     * @param message    The error message
     */
    public TargetReadOnlyException(@NotNull final EntityMappingInstructions mapping, @NotNull final String message) {
        super(mapping, String.format(exceptionMessage, message, mapping.getSourceEntityHeader().toStringVerbose()));
    }
}
