package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;

/**
 * This is used to create cluster properties depentent objects using the cluster property name as the public id.
 *
 * @author Victor Kazakov
 */
public class ClusterPropertyDependencyProcessor extends DefaultDependencyProcessor<ClusterProperty> {
    @NotNull
    @Override
    public DependentObject createDependentObject(@NotNull ClusterProperty clusterProperty) {
        return new DependentEntity(clusterProperty.getName(), EntityHeaderUtils.fromEntity(clusterProperty));
    }
}
