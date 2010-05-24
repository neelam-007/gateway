package com.l7tech.gateway.common.transport;

import javax.servlet.ServletRequest;

/**
 * Interface for location of an SsgConnector
 */
public interface SsgConnectorFinder {

    /**
     * Find the SsgConnector for the given servlet request.
     *
     * @param servletRequest The request to use
     * @return The SsgConnector or null if not found.
     */
    SsgConnector findSsgConnector( ServletRequest servletRequest );
}
