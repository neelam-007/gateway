package com.l7tech.external.assertions.generateoauthsignaturebasestring.server;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Thrown if an oauth parameter has been detected more than once.
 */
public class DuplicateParameterException extends ParameterException {
    private final List<String> duplicateValues;

    public DuplicateParameterException(@NotNull final String parameter, @NotNull final List<String> duplicateValues, @NotNull final String message) {
        super(parameter, message);
        this.duplicateValues = duplicateValues;
    }

    public List<String> getDuplicateValues() {
        return duplicateValues;
    }
}
