package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.JMSDestinationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.JMSDestinationMO;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class JMSDestinationTransformer extends APIResourceWsmanBaseTransformer<JMSDestinationMO, JmsEndpoint, JMSDestinationResourceFactory> {

    @Override
    @Inject
    protected void setFactory(JMSDestinationResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<JMSDestinationMO> convertToItem(JMSDestinationMO m) {
        return new ItemBuilder<JMSDestinationMO>(m.getJmsDestinationDetail().getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
