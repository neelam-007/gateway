package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.CustomKeyValueStoreResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.CustomKeyValueStoreMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.CustomKeyValueStore;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class CustomKeyValueStoreTransformer extends APIResourceWsmanBaseTransformer<CustomKeyValueStoreMO, CustomKeyValueStore, EntityHeader,CustomKeyValueStoreResourceFactory> {

    @Override
    @Inject
    @Named("customKeyValueStoreResourceFactory")
    protected void setFactory(CustomKeyValueStoreResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<CustomKeyValueStoreMO> convertToItem(@NotNull CustomKeyValueStoreMO m) {
        return new ItemBuilder<CustomKeyValueStoreMO>(m.getKey(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
