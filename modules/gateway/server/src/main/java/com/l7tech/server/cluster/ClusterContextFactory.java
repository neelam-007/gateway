package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ClusterContext;
import com.l7tech.gateway.common.spring.remoting.http.ConfigurableHttpInvokerRequestExecutor;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;

import java.util.Collection;

/**
 * Factory for cluster contexts.
 */
public class ClusterContextFactory {

    //- PUBLIC

    public ClusterContextFactory( final Collection<Object> remoteObjects,
                                  final ConfigurableHttpInvokerRequestExecutor configurableInvoker,
                                  final String permittedFacility ) {
        this.remoteObjects = remoteObjects;
        this.configurableInvoker = configurableInvoker;
        this.permittedFacility = permittedFacility;
    }

    public ClusterContext buildClusterContext( final String host,
                                               final int port ) {
        if ( permittedFacility != null ) {
            String facility = RemoteUtils.getFacility();
            if ( facility != null && !permittedFacility.equals(facility) ) {
                throw new IllegalStateException("Facility not permitted for facility '"+facility+"', must be '"+permittedFacility+"'.");    
            }
        }

        return new ClusterContextImpl( host, port, remoteObjects, configurableInvoker );
    }

    //- PRIVATE

    private final String permittedFacility;
    private final Collection<Object> remoteObjects;
    private final ConfigurableHttpInvokerRequestExecutor configurableInvoker;

}


