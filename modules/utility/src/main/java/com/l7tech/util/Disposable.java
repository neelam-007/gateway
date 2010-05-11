package com.l7tech.util;

/**
 * General purpose interface for releasing of resources.
 */
public interface Disposable {

    /**
     * Release any resources.
     * 
     * <p>Implementations should assume that dispose could be called multiple times.</p>
     *
     * <p>Exceptions should not be thrown.</p>
     */
    void dispose();
}
