package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ClusterContext;
import com.l7tech.gateway.common.log.LogAccessAdmin;
import com.l7tech.gateway.common.spring.remoting.http.RemotingContext;
import com.l7tech.gateway.common.spring.remoting.http.ConfigurableHttpInvokerRequestExecutor;

import java.util.Collection;

/**
 * Cluster context implementation.
 */
public class ClusterContextImpl extends RemotingContext implements ClusterContext {

    /**
     *
     */
    ClusterContextImpl( final String host,
                        final int port,
                        final Collection<Object> remoteObjects,
                        final ConfigurableHttpInvokerRequestExecutor configurableInvoker ) {
        super( host, port, "", remoteObjects, configurableInvoker );
    }

    @Override
    public LogAccessAdmin getLogAccessAdmin() throws SecurityException {
        return getRemoteInterfaceForEndpoint(LogAccessAdmin.class);
    }
}
