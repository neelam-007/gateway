package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * The active connector dependency processor. This will find default dependencies defined by the SsgActiveConnector and
 * then delegate to a dependency processor for the specific active connector. The dependency processor must be in the
 * given map.
 *
 * @author Victor Kazakov
 */
public class SsgActiveConnectorDependencyProcessor extends GenericDependencyProcessor<SsgActiveConnector> {

    @Inject
    @Named("ssgActiveConnectorDependencyProcessorRegistry")
    private DependencyProcessorRegistry processorRegistry;

    @Override
    @NotNull
    public List<Dependency> findDependencies(SsgActiveConnector activeConnector, DependencyFinder finder) throws FindException {
        //find the default dependencies
        List<Dependency> dependencies = super.findDependencies(activeConnector, finder);

        //delegate to the custom dependency processor for the SsgActiveConnector type.
        DependencyProcessor processor = processorRegistry.get(activeConnector.getType());
        if (processor != null) {
            //noinspection unchecked
            dependencies.addAll(processor.findDependencies(activeConnector, finder));
        } else {
            throw new FindException("Unknown active connector type: " + activeConnector.getType());
        }
        return dependencies;
    }
}
