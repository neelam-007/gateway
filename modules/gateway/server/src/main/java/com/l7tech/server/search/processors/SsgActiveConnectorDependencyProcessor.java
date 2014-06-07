package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

/**
 * The active connector dependency processor. This will find default dependencies defined by the SsgActiveConnector and
 * then delegate to a dependency processor for the specific active connector. The dependency processor must be in the
 * given map.
 *
 * @author Victor Kazakov
 */
public class SsgActiveConnectorDependencyProcessor extends DefaultDependencyProcessor<SsgActiveConnector> {

    @Inject
    @Named("ssgActiveConnectorDependencyProcessorRegistry")
    private DependencyProcessorRegistry processorRegistry;

    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull final SsgActiveConnector activeConnector, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        //find the default dependencies
        final List<Dependency> dependencies = super.findDependencies(activeConnector, finder);

        //delegate to the custom dependency processor for the SsgActiveConnector type.
        final DependencyProcessor processor = processorRegistry.get(activeConnector.getType());
        if (processor != null) {
            //noinspection unchecked
            dependencies.addAll(processor.findDependencies(activeConnector, finder));
        } else {
            throw new CannotRetrieveDependenciesException(activeConnector.getName(), SsgActiveConnector.class, activeConnector.getClass(), "Unknown active connector type: " + activeConnector.getType());
        }
        return dependencies;
    }

    @Override
    public void replaceDependencies(@NotNull final SsgActiveConnector activeConnector, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder) throws CannotReplaceDependenciesException {
        // replace the dependencies using the super.
        super.replaceDependencies(activeConnector, replacementMap, finder);

        //delegate to the custom dependency processor for the SsgActiveConnector type.
        final DependencyProcessor processor = processorRegistry.get(activeConnector.getType());
        if (processor != null) {
            //noinspection unchecked
            processor.replaceDependencies(activeConnector, replacementMap, finder);
        } else {
            throw new CannotReplaceDependenciesException(activeConnector.getName(), activeConnector.getId(), EntityType.SSG_ACTIVE_CONNECTOR.getEntityClass(), activeConnector.getClass(), "Unknown active connector type: " + activeConnector.getType());
        }
    }
}
