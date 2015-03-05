package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.CustomKeyValueStoreResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.CustomKeyValueStoreMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Component
public class CustomKeyValueStoreAPIResourceFactory extends WsmanBaseResourceFactory<CustomKeyValueStoreMO, CustomKeyValueStoreResourceFactory> {

    public CustomKeyValueStoreAPIResourceFactory() {
    }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.CUSTOM_KEY_VALUE_STORE.toString();
    }

    @Override
    @Inject
    @Named("customKeyValueStoreResourceFactory")
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.CustomKeyValueStoreResourceFactory factory) {
        super.factory = factory;
    }
}
