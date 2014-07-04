package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import org.apache.commons.collections.CollectionUtils;
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
    private DependencyProcessorRegistry<SsgActiveConnector> processorRegistry;

    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull final SsgActiveConnector activeConnector, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        //find the default dependencies
        final List<Dependency> dependencies = super.findDependencies(activeConnector, finder);

        //delegate to the custom dependency processor for the SsgActiveConnector type.
        final DependencyProcessor<SsgActiveConnector> processor = processorRegistry.get(activeConnector.getType());
        if (processor != null) {
            //noinspection unchecked
            dependencies.addAll(CollectionUtils.subtract(processor.findDependencies(activeConnector, finder), dependencies));
        }
        return dependencies;
    }

    @Override
    public void replaceDependencies(@NotNull final SsgActiveConnector activeConnector, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        // replace the dependencies using the super.
        super.replaceDependencies(activeConnector, replacementMap, finder, replaceAssertionsDependencies);

        //delegate to the custom dependency processor for the SsgActiveConnector type.
        final DependencyProcessor<SsgActiveConnector> processor = processorRegistry.get(activeConnector.getType());
        if (processor != null) {
            //noinspection unchecked
            processor.replaceDependencies(activeConnector, replacementMap, finder, replaceAssertionsDependencies);
        }
    }
}
