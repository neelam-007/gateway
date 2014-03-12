package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ListenPortResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ListenPortMO;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityHeader;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class ListenPortTransformer extends APIResourceWsmanBaseTransformer<ListenPortMO, SsgConnector, EntityHeader, ListenPortResourceFactory> {

    @Override
    @Inject
    protected void setFactory(ListenPortResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<ListenPortMO> convertToItem(ListenPortMO m) {
        return new ItemBuilder<ListenPortMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
