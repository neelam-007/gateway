package com.l7tech.console.util;

import com.l7tech.gateway.common.admin.AdminContext;
import com.l7tech.gateway.common.spring.remoting.http.ConfigurableHttpInvokerRequestExecutor;

import java.util.Collection;

/**
 * Factory for admin context
 */
public class AdminContextFactory {

    //- PUBLIC

    public AdminContextFactory( final Collection<Object> remoteObjects,
                                final ConfigurableHttpInvokerRequestExecutor configurableInvoker ) {
        this.remoteObjects = remoteObjects;
        this.configurableInvoker = configurableInvoker;
    }

    /**
     * Build and admin context with the given details.
     *
     * @param host The remote host
     * @param port The remote port
     * @param sessionId The session identifier to use.
     * @return The context to use for communication.
     */
    public AdminContext buildAdminContext( final String host,
                                           final int port,
                                           final String sessionId ) {
        return new AdminContextImpl( host, port, sessionId, remoteObjects, configurableInvoker );
    }

    //- PRIVATE

    private final Collection<Object> remoteObjects;
    private final ConfigurableHttpInvokerRequestExecutor configurableInvoker;
}
