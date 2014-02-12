package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PrivateKeyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PrivateKeyTransformer extends APIResourceWsmanBaseTransformer<PrivateKeyMO, SsgKeyEntry, PrivateKeyResourceFactory> {

    @Override
    @Inject
    protected void setFactory(PrivateKeyResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<PrivateKeyMO> convertToItem(PrivateKeyMO m) {
        return new ItemBuilder<PrivateKeyMO>(m.getAlias(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
