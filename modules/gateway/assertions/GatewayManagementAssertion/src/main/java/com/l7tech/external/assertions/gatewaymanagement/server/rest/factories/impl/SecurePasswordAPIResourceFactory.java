package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.SecurePasswordResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * The secure password rest resources factory
 */
@Component
public class SecurePasswordAPIResourceFactory extends WsmanBaseResourceFactory<StoredPasswordMO, SecurePasswordResourceFactory> {

    public SecurePasswordAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.SECURE_PASSWORD.toString();
    }

    @Override
    @Inject
    public void setFactory(SecurePasswordResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public StoredPasswordMO getResourceTemplate() {
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("Template Name");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", true)
                .put("description", "My Password Description")
                .put("type", "Password")
                .map());
        return storedPasswordMO;
    }
}
