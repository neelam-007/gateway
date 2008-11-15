package com.l7tech.gateway.config.client.beans;

/**
 * Interface that allows provision of a status code.
 */
public interface StatusCodeSource {

    /**
     * Get the status code.
     *
     * @return The code.
     */
    int getStatusCode();
}
