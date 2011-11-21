package com.l7tech.server.util;

/**
 * Dependency injection interface for object construction.
 */
public interface InjectingConstructor {

    /**
     * Create a new instanceof the given class
     *
     * @param type The bean class
     * @param <T> The bean type
     * @return The new instance
     */
    <T> T injectNew( Class<T> type );
}
