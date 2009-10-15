package com.l7tech.server;

/**
 * Component life cycle interface.
 */
public interface Lifecycle {

    /**
     * Start the component.
     *
     * @throws LifecycleException If an error occurs.
     */
    void start() throws LifecycleException;

    /**
     * Stop the component.
     *
     * @throws LifecycleException If an error occurs.
     */
    void stop() throws LifecycleException;
}
