package com.l7tech.server.policy.module;

/**
 * Thrown if an exception happens during module load process.
 */
public class ModuleException extends Exception {
    public ModuleException(String message) {
        super(message);
    }
    public ModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
