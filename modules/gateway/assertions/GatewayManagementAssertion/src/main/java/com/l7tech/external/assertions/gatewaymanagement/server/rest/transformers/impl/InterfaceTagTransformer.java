package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.InterfaceTagResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
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
    public EntityType getEntityType() {
        return factory.getType();
    }

    @Override
    public InterfaceTagMO convertToMO(InterfaceTag e) {
        return factory.internalAsResource(e);
    }

    @Override
    public InterfaceTag convertFromMO(InterfaceTagMO m) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(m, true);
    }

    @Override
    public InterfaceTag convertFromMO(InterfaceTagMO m, boolean strict) throws ResourceFactory.InvalidResourceException {
        //strict can be ignored here as interface tag does not depend on other entities.
        return factory.internalFromResource(m).left;
    }

    @Override
    public Item<InterfaceTagMO> convertToItem(EntityHeader header){
        return new ItemBuilder<InterfaceTagMO>(header.getName(), header.getStrId(), factory.getType().name())
                .build();
    }

    @Override
    public Item<InterfaceTagMO> convertToItem(InterfaceTagMO m) {
        return new ItemBuilder<InterfaceTagMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
