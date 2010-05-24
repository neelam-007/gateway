package com.l7tech.server.transport.http;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.SsgConnectorFinder;

import javax.servlet.ServletRequest;

/**
 * Default implementation of the SsgConnectorFinder
 */
public class DefaultSsgConnectorFinder implements SsgConnectorFinder {

    @Override
    public SsgConnector findSsgConnector( final ServletRequest servletRequest ) {
        return HttpTransportModule.getConnector( servletRequest );
    }
}
