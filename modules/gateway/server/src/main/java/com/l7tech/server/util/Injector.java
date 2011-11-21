package com.l7tech.server.util;

/**
 * Dependency injection interface.
 */
public interface Injector {

    /**
     * Inject values into an existing object.
     *
     * @param target The target object.
     */
    void inject( Object target );
}
