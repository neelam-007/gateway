package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * The connector dependency processor. This will find default dependencies defined by the SsgConnector and then delegate
 * to a dependency processor for the specific connector. The dependency processor must be in the given map.
 *
 * @author Victor Kazakov
 */
public class SsgConnectorDependencyProcessor extends GenericDependencyProcessor<SsgConnector> {

    private Map<String, DependencyProcessor<SsgConnector>> processors;

    /**
     * Creates a new SsgConnectorDependencyProcessor given the map of dependency processors to use for the different
     * connectors
     *
     * @param processors The map of dependency processors to use of the different connector schemes
     */
    public SsgConnectorDependencyProcessor(Map<String, DependencyProcessor<SsgConnector>> processors) {
        this.processors = processors;
    }

    @Override
    @NotNull
    public List<Dependency> findDependencies(SsgConnector activeConnector, DependencyFinder finder) throws FindException {
        //finds the default SsgConnector dependencies
        List<Dependency> dependencies = super.findDependencies(activeConnector, finder);

        //Gets the dependencies from the dependency processor for the connector scheme
        DependencyProcessor processor = processors.get(activeConnector.getScheme());
        if (processor != null) {
            //noinspection unchecked
            dependencies.addAll(processor.findDependencies(activeConnector, finder));
        } else {
            throw new FindException("Unknown connector type: " + activeConnector.getScheme());
        }
        return dependencies;
    }
}
