package com.l7tech.server.bundling.exceptions;

import com.l7tech.server.bundling.EntityMappingInstructions;
import org.jetbrains.annotations.NotNull;

/**
 * This is a base exception that bundle import exceptions are based on.
 */
public abstract class BundleImportException extends Exception {
    private static final long serialVersionUID = -2538183969847789505L;

    //The incorrect mapping instructions
    @NotNull
    private final EntityMappingInstructions mapping;

    /**
     * Create a new bundle import exception.
     *
     * @param mapping The mapping instructions that are incorrect
     * @param message The error message
     */
    public BundleImportException(@NotNull final EntityMappingInstructions mapping, @NotNull final String message) {
        super(message);
        this.mapping = mapping;
    }

    /**
     * Create a new bundle import exception.
     *
     * @param mapping The mapping instructions that are incorrect
     * @param message The error message
     * @param t       The associated error with this mapping exception
     */
    public BundleImportException(@NotNull final EntityMappingInstructions mapping, @NotNull final String message, @NotNull final Throwable t) {
        super(message, t);
        this.mapping = mapping;
    }


    /**
     * Returns the incorrect mapping instructions
     *
     * @return The incorrect mapping instructions
     */
    @NotNull
    public EntityMappingInstructions getMapping() {
        return mapping;
    }
}
