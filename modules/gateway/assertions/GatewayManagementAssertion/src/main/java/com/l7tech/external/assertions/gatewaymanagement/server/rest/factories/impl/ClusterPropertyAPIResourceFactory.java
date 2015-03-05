package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ClusterPropertyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ClusterPropertyMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

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
    @Named("clusterPropertyResourceFactory")
    public void setFactory(ClusterPropertyResourceFactory factory) {
        super.factory = factory;
    }
}