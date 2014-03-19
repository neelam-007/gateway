package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.InterfaceTagResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class InterfaceTagAPIResourceFactory extends WsmanBaseResourceFactory<InterfaceTagMO, InterfaceTagResourceFactory> {

    public InterfaceTagAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return "INTERFACE_TAG";
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.InterfaceTagResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public InterfaceTagMO getResourceTemplate() {
        InterfaceTagMO interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setName("TemplateInterfaceTag");
        interfaceTagMO.setAddressPatterns(CollectionUtils.list("0.0.0.0/00"));
        return interfaceTagMO;
    }
}
