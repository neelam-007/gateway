package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.CustomKeyValueStoreResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.CustomKeyValueStoreMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

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
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.CustomKeyValueStoreResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public CustomKeyValueStoreMO getResourceTemplate() {
        CustomKeyValueStoreMO keyValueMO = ManagedObjectFactory.createCustomKeyValueStore();
        keyValueMO.setKey("TemplateKey");
        keyValueMO.setStoreName(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
        keyValueMO.setValue("TemplateValue".getBytes());
        return keyValueMO;
    }
}
