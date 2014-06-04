package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;

/**
 * This is used to find dependencies in the jms endpoint and the related jms connection object
 */
public class JmsEndpointDependencyProcessor extends DefaultDependencyProcessor<JmsEndpoint> implements DependencyProcessor<JmsEndpoint> {

    @Inject
    private JmsConnectionManager jmsConnectionManager;

    private DefaultDependencyProcessor<JmsConnection> jmsConnectionDefaultDependencyProcessor = new DefaultDependencyProcessor<>();

    /**
     * Finds the dependencies that the jms endpoint has and the associated jms connection has.
     *
     * @param endpoint The jms endpoint to find dependencies for.
     * @param finder   The finder that if performing the current dependency search
     * @return The list of dependencies that this assertion has
     * @throws FindException This is thrown if an entity cannot be found
     */
    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull final JmsEndpoint endpoint, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        //dependencies for the jms endpoint
        final List<Dependency> dependencies = super.findDependencies(endpoint, finder);

        //dependencies for the associated jms connection
        final JmsConnection jmsConnection = jmsConnectionManager.findByPrimaryKey(endpoint.getConnectionGoid());
        if (jmsConnection != null) {
            dependencies.addAll(jmsConnectionDefaultDependencyProcessor.findDependencies(jmsConnection, finder));
        }

        return dependencies;
    }
}
