package com.l7tech.external.assertions.generateoauthsignaturebasestring.server;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown if a required oauth parameter is missing.
 */
public class MissingRequiredParameterException extends Exception {
    private final String parameter;

    public MissingRequiredParameterException(@NotNull final String parameter, @NotNull final String message) {
        super(message);
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
