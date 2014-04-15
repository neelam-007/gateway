package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.GenericEntityResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class GenericEntityAPIResourceFactory extends WsmanBaseResourceFactory<GenericEntityMO, GenericEntityResourceFactory> {

    public GenericEntityAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.GENERIC.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.GenericEntityResourceFactory factory) {
        super.factory = factory;
    }
}
