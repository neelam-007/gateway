package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.CustomKeyValueStoreRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.CustomKeyValueStoreMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The custom key value store resource
 */
@Provider
@Path(CustomKeyValueStoreResource.customKeyValue_URI)
public class CustomKeyValueStoreResource extends RestEntityResource<CustomKeyValueStoreMO, CustomKeyValueStoreRestResourceFactory> {

    protected static final String customKeyValue_URI = "customKeyValues";

    @Override
    @SpringBean
    public void setFactory(CustomKeyValueStoreRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.CUSTOM_KEY_VALUE_STORE;
    }

    @Override
    protected Reference toReference(CustomKeyValueStoreMO resource) {
        return toReference(resource.getId(), resource.getStoreName());
    }
}
