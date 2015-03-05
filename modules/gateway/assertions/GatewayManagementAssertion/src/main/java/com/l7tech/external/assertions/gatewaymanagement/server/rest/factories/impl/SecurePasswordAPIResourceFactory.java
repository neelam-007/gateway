package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.SecurePasswordResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

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
    @Named("securePasswordResourceFactory")
    public void setFactory(SecurePasswordResourceFactory factory) {
        super.factory = factory;
    }
}
