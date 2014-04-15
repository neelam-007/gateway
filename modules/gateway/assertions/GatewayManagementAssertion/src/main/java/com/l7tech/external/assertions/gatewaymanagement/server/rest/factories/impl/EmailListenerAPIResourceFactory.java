package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EmailListenerResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class EmailListenerAPIResourceFactory extends WsmanBaseResourceFactory<EmailListenerMO, EmailListenerResourceFactory> {

    public EmailListenerAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.EMAIL_LISTENER.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.EmailListenerResourceFactory factory) {
        super.factory = factory;
    }
}
