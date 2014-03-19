package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ClusterPropertyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ClusterPropertyMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class ClusterPropertyAPIResourceFactory extends WsmanBaseResourceFactory<ClusterPropertyMO, ClusterPropertyResourceFactory> {

    public ClusterPropertyAPIResourceFactory() {
    }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.CLUSTER_PROPERTY.toString();
    }

    @Override
    @Inject
    public void setFactory(ClusterPropertyResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public ClusterPropertyMO getResourceTemplate() {
        ClusterPropertyMO clusterPropertyMO = ManagedObjectFactory.createClusterProperty();
        clusterPropertyMO.setName("Template Cluster Property Name");
        clusterPropertyMO.setValue("Template Cluster Property Value");
        return clusterPropertyMO;
    }
}