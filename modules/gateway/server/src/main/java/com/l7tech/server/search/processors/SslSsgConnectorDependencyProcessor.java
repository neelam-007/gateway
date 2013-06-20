package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SsgKeyHeader;
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
    public List<Dependency> findDependencies(SsgConnector connector, DependencyFinder finder) throws FindException {
        String keyAlias = connector.getKeyAlias();
        Long keyStoreId = connector.getKeystoreOid();
        //add the ssg private key as a dependency
        List<Entity> dependentEntities = finder.retrieveEntities(new SsgKeyHeader(keyStoreId + ":" + keyAlias, keyStoreId == null ? -1 : keyStoreId, keyAlias, keyAlias), com.l7tech.search.Dependency.DependencyType.SSG_PRIVATE_KEY, com.l7tech.search.Dependency.MethodReturnType.ENTITY_HEADER);
        return finder.getDependenciesFromEntities(connector, finder, dependentEntities);
    }
}
