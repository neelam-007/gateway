package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServiceAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Component
public class ServiceAliasAPIResourceFactory extends WsmanBaseResourceFactory<ServiceAliasMO, ServiceAliasResourceFactory> {

    public ServiceAliasAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.SERVICE_ALIAS.toString();
    }

    @Override
    @Inject
    @Named("serviceAliasResourceFactory")
    public void setFactory(ServiceAliasResourceFactory factory) {
        super.factory = factory;
    }
}
