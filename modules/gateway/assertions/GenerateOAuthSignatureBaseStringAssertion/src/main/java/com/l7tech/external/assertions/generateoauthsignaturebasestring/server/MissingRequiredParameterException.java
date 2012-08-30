package com.l7tech.external.assertions.generateoauthsignaturebasestring.server;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown if a required oauth parameter is missing.
 */
public class MissingRequiredParameterException extends ParameterException {
    public MissingRequiredParameterException(@NotNull final String parameter, @NotNull final String message) {
        super(parameter, message);
    }
}
