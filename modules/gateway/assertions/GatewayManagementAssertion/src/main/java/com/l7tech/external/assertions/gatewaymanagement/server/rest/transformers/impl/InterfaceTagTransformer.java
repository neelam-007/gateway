package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.InterfaceTagResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.bundling.EntityContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class InterfaceTagTransformer implements APITransformer<InterfaceTagMO, InterfaceTag> {

    /**
     * The wiseman resource factory
     */
    protected InterfaceTagResourceFactory factory;

    @Inject
    protected void setFactory(InterfaceTagResourceFactory factory) {
        this.factory = factory;
    }

    @NotNull
    @Override
    public String getResourceType() {
        return "INTERFACE_TAG";
    }

    @Override
    public InterfaceTagMO convertToMO(InterfaceTag e) {
        return factory.internalAsResource(e);
    }

    @Override
    public EntityContainer<InterfaceTag> convertFromMO(InterfaceTagMO interfaceTagMO) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(interfaceTagMO,true);
    }

    @Override
    public EntityContainer<InterfaceTag> convertFromMO(InterfaceTagMO m, boolean strict) throws ResourceFactory.InvalidResourceException {
        return new EntityContainer<>(factory.internalFromResource(m).left);
    }

    @Override
    public EntityHeader convertToHeader(InterfaceTagMO m) throws ResourceFactory.InvalidResourceException {
        return new EntityHeader(m.getId(), factory.getType(), m.getName(), null, m.getVersion());
    }


    @Override
    public Item<InterfaceTagMO> convertToItem(EntityHeader header){
        return new ItemBuilder<InterfaceTagMO>(header.getName(), header.getStrId(), factory.getType().name())
                .build();
    }

    @Override
    public Item<InterfaceTagMO> convertToItem(InterfaceTagMO m) {
        return new ItemBuilder<InterfaceTagMO>(m.getName(), m.getId(), getResourceType())
                .setContent(m)
                .build();
    }
}
