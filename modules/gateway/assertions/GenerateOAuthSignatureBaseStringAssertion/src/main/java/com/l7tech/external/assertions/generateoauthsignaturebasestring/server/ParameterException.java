package com.l7tech.external.assertions.generateoauthsignaturebasestring.server;

import org.jetbrains.annotations.NotNull;

public class ParameterException extends Exception {
    private final String parameter;

    public ParameterException(@NotNull final String parameter, @NotNull final String message) {
        super(message);
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
