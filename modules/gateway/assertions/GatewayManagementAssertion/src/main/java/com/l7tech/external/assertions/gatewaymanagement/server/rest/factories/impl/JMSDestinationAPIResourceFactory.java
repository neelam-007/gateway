package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.JMSDestinationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.JMSDestinationMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class JMSDestinationAPIResourceFactory extends WsmanBaseResourceFactory<JMSDestinationMO, JMSDestinationResourceFactory> {

    public JMSDestinationAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.JMS_ENDPOINT.toString();
    }

    @Override
    @Inject
    public void setFactory(JMSDestinationResourceFactory factory) {
        super.factory = factory;
    }
}
