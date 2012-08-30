package com.l7tech.external.assertions.generateoauthsignaturebasestring.server;

import org.jetbrains.annotations.NotNull;

public class InvalidParameterException extends ParameterException {
    private final String invalidValue;

    public InvalidParameterException(@NotNull final String parameter, @NotNull final String invalidValue, @NotNull final String message) {
        super(parameter, message);
        this.invalidValue = invalidValue;
    }

    public String getInvalidValue() {
        return invalidValue;
    }
}
