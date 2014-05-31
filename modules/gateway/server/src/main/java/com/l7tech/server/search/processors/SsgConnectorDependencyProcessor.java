package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * The connector dependency processor. This will find default dependencies defined by the SsgConnector and then delegate
 * to a dependency processor for the specific connector. The dependency processor must be in the given map.
 *
 * @author Victor Kazakov
 */
public class SsgConnectorDependencyProcessor extends DefaultDependencyProcessor<SsgConnector> {

    @Inject
    @Named("ssgConnectorDependencyProcessorRegistry")
    private DependencyProcessorRegistry ssgConnectorDependencyProcessorRegistry;

    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull SsgConnector activeConnector, @NotNull DependencyFinder finder) throws FindException {
        //finds the default SsgConnector dependencies
        List<Dependency> dependencies = super.findDependencies(activeConnector, finder);

        //Gets the dependencies from the dependency processor for the connector scheme
        DependencyProcessor processor = ssgConnectorDependencyProcessorRegistry.get(activeConnector.getScheme());
        if (processor != null) {
            //noinspection unchecked
            dependencies.addAll(processor.findDependencies(activeConnector, finder));
        } else {
            throw new FindException("Unknown connector type: " + activeConnector.getScheme());
        }
        return dependencies;
    }
}
