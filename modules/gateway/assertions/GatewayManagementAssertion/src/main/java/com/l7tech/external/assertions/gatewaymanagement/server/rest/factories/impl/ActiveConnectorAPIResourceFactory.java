package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ActiveConnectorResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ActiveConnectorMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Component
public class ActiveConnectorAPIResourceFactory extends WsmanBaseResourceFactory<ActiveConnectorMO, ActiveConnectorResourceFactory> {

    public ActiveConnectorAPIResourceFactory() {
    }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.SSG_ACTIVE_CONNECTOR.toString();
    }

    @Override
    @Inject
    @Named("activeConnectorResourceFactory")
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.ActiveConnectorResourceFactory factory) {
        super.factory = factory;
    }
}
