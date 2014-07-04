package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.SsgConnector;
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
 * The connector dependency processor. This will find default dependencies defined by the SsgConnector and then delegate
 * to a dependency processor for the specific connector. The dependency processor must be in the given map.
 *
 * @author Victor Kazakov
 */
public class SsgConnectorDependencyProcessor extends DefaultDependencyProcessor<SsgConnector> {

    @Inject
    @Named("ssgConnectorDependencyProcessorRegistry")
    private DependencyProcessorRegistry<SsgConnector> ssgConnectorDependencyProcessorRegistry;

    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull final SsgConnector connector, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        //finds the default SsgConnector dependencies
        final List<Dependency> dependencies = super.findDependencies(connector, finder);

        //Gets the dependencies from the dependency processor for the connector scheme
        final DependencyProcessor<SsgConnector> processor = ssgConnectorDependencyProcessorRegistry.get(connector.getScheme());
        if (processor != null) {
            //noinspection unchecked
            dependencies.addAll(CollectionUtils.subtract(processor.findDependencies(connector, finder), dependencies));
        }
        return dependencies;
    }

    @Override
    public void replaceDependencies(@NotNull final SsgConnector connector, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        // replace the dependencies using the super.
        super.replaceDependencies(connector, replacementMap, finder, replaceAssertionsDependencies);

        //delegate to the custom dependency processor for the SsgActiveConnector type.
        final DependencyProcessor<SsgConnector> processor = ssgConnectorDependencyProcessorRegistry.get(connector.getScheme());
        if (processor != null) {
            //noinspection unchecked
            processor.replaceDependencies(connector, replacementMap, finder, replaceAssertionsDependencies);
        }
    }
}
