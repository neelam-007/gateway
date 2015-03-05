package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ListenPortResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ListenPortMO;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class ListenPortTransformer extends APIResourceWsmanBaseTransformer<ListenPortMO, SsgConnector, EntityHeader, ListenPortResourceFactory> {

    @Override
    @Inject
    @Named("listenPortResourceFactory")
    protected void setFactory(ListenPortResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<ListenPortMO> convertToItem(@NotNull ListenPortMO m) {
        return new ItemBuilder<ListenPortMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
