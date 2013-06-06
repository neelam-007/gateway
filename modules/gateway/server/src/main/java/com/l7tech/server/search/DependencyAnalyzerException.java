package com.l7tech.server.search;

/**
 * Dependence analyzer exception. This is thrown if there was an exception finding an entities dependencies.
 *
 * @author Victor Kazakov
 */
public class DependencyAnalyzerException extends Exception {
    public DependencyAnalyzerException(String message) {
        super(message);
    }

    public DependencyAnalyzerException(String message, Throwable cause) {
        super(message, cause);
    }
}
