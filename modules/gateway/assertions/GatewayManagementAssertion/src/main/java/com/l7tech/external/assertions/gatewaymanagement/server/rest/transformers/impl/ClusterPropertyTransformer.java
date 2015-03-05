package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ClusterPropertyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.ClusterPropertyMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class ClusterPropertyTransformer extends APIResourceWsmanBaseTransformer<ClusterPropertyMO, ClusterProperty, EntityHeader, ClusterPropertyResourceFactory> {

    @Override
    @Inject
    @Named("clusterPropertyResourceFactory")
    protected void setFactory(ClusterPropertyResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<ClusterPropertyMO> convertToItem(@NotNull ClusterPropertyMO m) {
        return new ItemBuilder<ClusterPropertyMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
