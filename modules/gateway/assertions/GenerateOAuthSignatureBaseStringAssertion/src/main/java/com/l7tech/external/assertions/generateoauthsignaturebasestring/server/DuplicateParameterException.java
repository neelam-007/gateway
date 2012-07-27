package com.l7tech.external.assertions.generateoauthsignaturebasestring.server;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown if an oauth parameter has been detected more than once.
 */
public class DuplicateParameterException extends Exception {
    private final String parameter;

    public DuplicateParameterException(@NotNull final String parameter, @NotNull final String message) {
        super(message);
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
