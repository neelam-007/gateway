package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * This is a dependency processor for SsgConnector's that support ssl and depend on an SSG_PRIVATE_KEY
 *
 * @author Victor Kazakov
 */
public class SslSsgConnectorDependencyProcessor implements DependencyProcessor<SsgConnector> {
    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull final SsgConnector connector, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        //add the ssg private key as a dependency
        final List<Object> dependentEntities = finder.retrieveObjects(getSsgKeyHeader(connector), com.l7tech.search.Dependency.DependencyType.SSG_PRIVATE_KEY, com.l7tech.search.Dependency.MethodReturnType.ENTITY_HEADER);
        return finder.getDependenciesFromObjects(connector, finder, dependentEntities);
    }

    @Override
    public void replaceDependencies(@NotNull final SsgConnector connector, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        final SsgKeyHeader currentHeader = getSsgKeyHeader(connector);
        final EntityHeader replacementHeader = DependencyProcessorUtils.findMappedHeader(replacementMap,  currentHeader);
        if(replacementHeader != null){
            if(replacementHeader instanceof SsgKeyHeader) {
                connector.setKeyAlias(((SsgKeyHeader) replacementHeader).getAlias());
                connector.setKeystoreGoid(((SsgKeyHeader) replacementHeader).getKeystoreId());
            } else {
                throw new CannotReplaceDependenciesException(currentHeader.getName(), currentHeader.getStrId(), currentHeader.getType().getEntityClass(), connector.getClass(), "Expected mapped header for a private key to be of type SsgKeyHeader");
            }
        }
    }

    private SsgKeyHeader getSsgKeyHeader(@NotNull final SsgConnector connector) {
        return new SsgKeyHeader(connector.getKeystoreGoid() + ":" + connector.getKeyAlias(), connector.getKeystoreGoid() == null ? PersistentEntity.DEFAULT_GOID : connector.getKeystoreGoid(), connector.getKeyAlias(), connector.getKeyAlias());
    }
}
