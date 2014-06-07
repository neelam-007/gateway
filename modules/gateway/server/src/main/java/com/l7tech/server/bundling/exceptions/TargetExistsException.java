package com.l7tech.server.bundling.exceptions;

import com.l7tech.server.bundling.EntityMappingInstructions;
import org.jetbrains.annotations.NotNull;

/**
 * This is thrown if a target entity exists but it is not supposed to according to the mapping instructions.
 */
public class TargetExistsException extends BundleImportException {
    private static final long serialVersionUID = -4331634283027432798L;

    private static final String exceptionMessage = "Target entity exists but was not expected: %1$s. Source Entity: %2$s";

    /**
     * Create a new target exists mapping exception.
     *
     * @param mapping The mapping instructions that map to the existing
     * @param message The error message
     */
    public TargetExistsException(@NotNull final EntityMappingInstructions mapping, @NotNull final String message) {
        super(mapping, String.format(exceptionMessage, message, mapping.getSourceEntityHeader().toStringVerbose()));
    }
}
