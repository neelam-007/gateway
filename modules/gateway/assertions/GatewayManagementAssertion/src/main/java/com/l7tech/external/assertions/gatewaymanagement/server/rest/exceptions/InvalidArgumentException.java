package com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This exception is thrown when a Rest resource is given an invalid argument
 */
public class InvalidArgumentException extends RuntimeException {
    @Nullable
    private final String argumentName;

    /**
     * Create a new invalid argument exception with this given error message
     *
     * @param message The message describing why the argument is invalid
     */
    public InvalidArgumentException(@NotNull String message) {
        super(message);
        this.argumentName = null;
    }

    /**
     * Create a new invalid argument exception with the argument name and error message.
     *
     * @param argumentName The name of the invalid argument
     * @param message      The message describing why the argument is invalid
     */
    public InvalidArgumentException(@SuppressWarnings("NullableProblems") @NotNull String argumentName, @NotNull String message) {
        super(message);
        this.argumentName = argumentName;
    }

    /**
     * Returns the name of the invalid argument
     *
     * @return The name of the invalid argument
     */
    @Nullable
    public String getArgumentName() {
        return argumentName;
    }
}
