package com.l7tech.gateway.common.module;

/**
 * Signals that {@link com.l7tech.gateway.common.module.ServerModuleFile Server Module File}
 * loading exception of some sort has occurred.
 */
@SuppressWarnings({"serial", "UnusedDeclaration"})
public class ModuleLoadingException extends Exception {
    public ModuleLoadingException() {
        super();
    }

    public ModuleLoadingException(final String message) {
        super(message);
    }

    public ModuleLoadingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ModuleLoadingException(final Throwable cause) {
        super(cause);
    }
}
