package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EmailListenerResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class EmailListenerTransformer extends APIResourceWsmanBaseTransformer<EmailListenerMO, EmailListener, EntityHeader, EmailListenerResourceFactory> {

    @Override
    @Inject
    @Named("emailListenerResourceFactory")
    protected void setFactory(EmailListenerResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<EmailListenerMO> convertToItem(@NotNull EmailListenerMO m) {
        return new ItemBuilder<EmailListenerMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
