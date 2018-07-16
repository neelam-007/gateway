package com.ca.apim.gateway.extension.lifecycle;

import com.ca.apim.gateway.extension.Extension;

/**
 * Defines an extension to add functionality to the startup and shutdown process of the Gateway.
 */
public interface LifecycleAwareExtension extends Extension {

    /**
     * This is called after all extensions and modules are loaded but before the gateway has officially started.
     * Implement code to start your extensions in here.
     *
     * @throws LifecycleException Throw a lifecycle exception in order to prevent the gateway from starting. This will
     *                            cause the gateway to shutdown.
     */
    void start() throws LifecycleException;

    /**
     * This is called when the gateway is being shutdown in order to cleanly stop all extensions.
     *
     * @throws LifecycleException If there was some exception stopping the extension through a lifecycle exception
     */
    void stop() throws LifecycleException;

    /**
     * Called to fetch the name of a particular extension (generally for auditing purposes)
     *
     * @return the name of the lifecycle aware extension
     */
    String getName();
}
