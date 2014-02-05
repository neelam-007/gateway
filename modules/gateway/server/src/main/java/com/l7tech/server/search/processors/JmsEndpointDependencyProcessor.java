package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;

/**
* This is used to find dependencies in the jms endpoint and the related jms connection object
*
*/
public class JmsEndpointDependencyProcessor extends GenericDependencyProcessor<JmsEndpoint> implements DependencyProcessor<JmsEndpoint> {

    @Inject
    private JmsConnectionManager jmsConnectionManager;

    private GenericDependencyProcessor<JmsConnection> jmsConnectionGenericDependencyProcessor = new GenericDependencyProcessor<JmsConnection>();

    /**
     * Finds the dependencies that the jms endpoint has and the associated jms connection has.
     *
     * @param endpoint  The jms endpoint to find dependencies for.
     * @param finder    The finder that if performing the current dependency search
     * @return The list of dependencies that this assertion has
     * @throws FindException This is thrown if an entity cannot be found
     */
    @NotNull
    @Override
    public List<Dependency> findDependencies(JmsEndpoint endpoint, DependencyFinder finder) throws FindException {
        //dependencies for the jms endpoint
        List<Dependency> dependencies = super.findDependencies(endpoint, finder);

        //dependencies for the associated jms connection
        dependencies.addAll(jmsConnectionGenericDependencyProcessor.findDependencies(jmsConnectionManager.findByPrimaryKey(endpoint.getConnectionGoid()), finder));

        return dependencies;
    }
}
