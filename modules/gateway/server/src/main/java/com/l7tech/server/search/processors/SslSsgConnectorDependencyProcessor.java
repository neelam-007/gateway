package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.*;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * This is a dependency processor for SsgConnector's that support ssl and depend on an SSG_PRIVATE_KEY
 *
 * @author Victor Kazakov
 */
public class SslSsgConnectorDependencyProcessor extends BaseDependencyProcessor<SsgConnector> {
    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull final SsgConnector connector, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        final String keyAlias = connector.getKeyAlias();
        final Goid keyStoreId = connector.getKeystoreGoid();
        //add the ssg private key as a dependency
        final List<Object> dependentEntities = finder.retrieveObjects(new SsgKeyHeader(keyStoreId + ":" + keyAlias, keyStoreId == null ? PersistentEntity.DEFAULT_GOID : keyStoreId, keyAlias, keyAlias), com.l7tech.search.Dependency.DependencyType.SSG_PRIVATE_KEY, com.l7tech.search.Dependency.MethodReturnType.ENTITY_HEADER);
        return finder.getDependenciesFromObjects(connector, finder, dependentEntities);
    }
}
