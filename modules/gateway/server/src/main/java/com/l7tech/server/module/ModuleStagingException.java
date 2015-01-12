package com.l7tech.server.module;

/**
 * Signals that {@link com.l7tech.gateway.common.module.ServerModuleFile Server Module File}
 * staging exception of some sort has occurred.
 */
@SuppressWarnings({"serial", "UnusedDeclaration"})
public class ModuleStagingException extends Exception {
    public ModuleStagingException() {
        super();
    }

    public ModuleStagingException(final String message) {
        super(message);
    }

    public ModuleStagingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ModuleStagingException(final Throwable cause) {
        super(cause);
    }
}
