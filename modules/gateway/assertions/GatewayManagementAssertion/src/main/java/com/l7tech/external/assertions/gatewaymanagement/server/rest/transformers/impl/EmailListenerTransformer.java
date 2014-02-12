package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EmailListenerResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.common.transport.email.EmailListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class EmailListenerTransformer extends APIResourceWsmanBaseTransformer<EmailListenerMO, EmailListener, EmailListenerResourceFactory> {

    @Override
    @Inject
    protected void setFactory(EmailListenerResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<EmailListenerMO> convertToItem(EmailListenerMO m) {
        return new ItemBuilder<EmailListenerMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
