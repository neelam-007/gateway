package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ListenPortResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ListenPortMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Component
public class ListenPortAPIResourceFactory extends WsmanBaseResourceFactory<ListenPortMO, ListenPortResourceFactory> {

    public ListenPortAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.SSG_CONNECTOR.toString();
    }

    @Override
    @Inject
    @Named("listenPortResourceFactory")
    public void setFactory(ListenPortResourceFactory factory) {
        super.factory = factory;
    }
}
