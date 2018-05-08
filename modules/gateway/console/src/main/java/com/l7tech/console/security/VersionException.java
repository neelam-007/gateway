package com.l7tech.console.security;

import org.jetbrains.annotations.NotNull;

/**
 * Exception caused by version mismatch.
 * May contain version info.
 */
public class VersionException extends Exception {
    /**
     * Standard exception constructor.
     */
    public VersionException(@NotNull final String message) {
        super(message);
    }

}
