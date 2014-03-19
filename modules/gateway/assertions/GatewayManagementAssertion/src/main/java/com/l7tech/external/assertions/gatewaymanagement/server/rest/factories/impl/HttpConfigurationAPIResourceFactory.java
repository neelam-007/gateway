package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.HttpConfigurationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.HttpConfigurationMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class HttpConfigurationAPIResourceFactory extends WsmanBaseResourceFactory<HttpConfigurationMO, HttpConfigurationResourceFactory> {

    public HttpConfigurationAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.HTTP_CONFIGURATION.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.HttpConfigurationResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public HttpConfigurationMO getResourceTemplate() {
        HttpConfigurationMO httpConfigurationMO = ManagedObjectFactory.createHttpConfiguration();
        httpConfigurationMO.setHost("TemplateHostname");
        httpConfigurationMO.setPort(1234);
        httpConfigurationMO.setPath("TemplatePath");
        return httpConfigurationMO;
    }
}