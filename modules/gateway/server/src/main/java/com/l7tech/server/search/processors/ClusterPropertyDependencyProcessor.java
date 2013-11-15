package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;

/**
 * This is used to create cluster properties depentent objects using the cluster property name as the public id.
 *
 * @author Victor Kazakov
 */
public class ClusterPropertyDependencyProcessor extends GenericDependencyProcessor<ClusterProperty> {
    @Override
    public DependentObject createDependentObject(ClusterProperty clusterProperty) {
        return new DependentEntity(clusterProperty.getName(), EntityType.CLUSTER_PROPERTY, clusterProperty.getId(), clusterProperty.getName());
    }
}
