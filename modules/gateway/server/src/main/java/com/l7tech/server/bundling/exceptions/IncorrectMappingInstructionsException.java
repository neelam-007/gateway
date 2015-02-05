package com.l7tech.server.bundling.exceptions;

import com.l7tech.server.bundling.EntityMappingInstructions;
import org.jetbrains.annotations.NotNull;

/**
 * This is thrown if there mapping instructions are improperly specified when importing a bundle
 */
public class IncorrectMappingInstructionsException extends BundleImportException {
    private static final long serialVersionUID = 3836314954706329670L;

    private static final String exceptionMessage = "Incorrect mapping instructions: %1$s. Mapping for: %2$s";

    /**
     * Create a new incorrect mapping exception.
     *
     * @param mapping The mapping instructions that are incorrect
     * @param message The error message
     */
    public IncorrectMappingInstructionsException(@NotNull final EntityMappingInstructions mapping, @NotNull final String message) {
        super(mapping, String.format(exceptionMessage, message, mapping.getSourceEntityHeader().toStringVerbose()));
    }

    /**
     * Create a new incorrect mapping exception.
     *
     * @param mapping The mapping instructions that are incorrect
     * @param message The error message
     * @param t       The associated error with this mapping exception
     */
    public IncorrectMappingInstructionsException(@NotNull final EntityMappingInstructions mapping, @NotNull final String message, @NotNull final Throwable t) {
        super(mapping, String.format(exceptionMessage, message, mapping.getSourceEntityHeader().toStringVerbose()), t);
    }
}
