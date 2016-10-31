package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PasswordPolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.PasswordPolicyMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * The password rule rest resources factory
 */
@Component
public class PasswordPolicyAPIResourceFactory extends WsmanBaseResourceFactory<PasswordPolicyMO, PasswordPolicyResourceFactory> {

    public PasswordPolicyAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.PASSWORD_POLICY.toString();
    }

    @Override
    @Inject
    @Named("passwordPolicyResourceFactory")
    public void setFactory(PasswordPolicyResourceFactory factory) {
        super.factory = factory;
    }
}
