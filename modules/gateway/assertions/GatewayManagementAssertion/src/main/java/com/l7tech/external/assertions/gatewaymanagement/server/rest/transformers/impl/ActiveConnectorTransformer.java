package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ActiveConnectorResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.ActiveConnectorMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnectorHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class ActiveConnectorTransformer extends APIResourceWsmanBaseTransformer<ActiveConnectorMO, SsgActiveConnector, SsgActiveConnectorHeader, ActiveConnectorResourceFactory> {

    @Override
    @Inject
    @Named("activeConnectorResourceFactory")
    protected void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.ActiveConnectorResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<ActiveConnectorMO> convertToItem(@NotNull ActiveConnectorMO m) {
        return new ItemBuilder<ActiveConnectorMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
