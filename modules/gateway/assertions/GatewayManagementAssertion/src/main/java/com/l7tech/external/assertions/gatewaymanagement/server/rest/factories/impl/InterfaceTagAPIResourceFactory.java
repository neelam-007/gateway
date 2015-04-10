package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.InterfaceTagResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

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
    @Named("interfaceTagResourceFactory")
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.InterfaceTagResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public InterfaceTagMO getResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException {
        //strip out the interface tag version. It reflects the cluster property version. SSG-8178
        InterfaceTagMO interfaceTag = super.getResource(id);
        interfaceTag.setVersion(null);
        return interfaceTag;
    }

    @Override
    public List<InterfaceTagMO> listResources(@Nullable String sort, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> filters) {
        //strip out the interface tag version. It reflects the cluster property version. SSG-8178
        return Functions.map(super.listResources(sort, ascending, filters), new Functions.Unary<InterfaceTagMO, InterfaceTagMO>() {
            @Override
            public InterfaceTagMO call(InterfaceTagMO interfaceTagMO) {
                interfaceTagMO.setVersion(null);
                return interfaceTagMO;
            }
        });
    }
}
