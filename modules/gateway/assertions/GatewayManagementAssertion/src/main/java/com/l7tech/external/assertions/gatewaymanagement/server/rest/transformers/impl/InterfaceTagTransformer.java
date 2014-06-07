package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.InterfaceTagResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.InterfaceTagWrapper;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.server.bundling.EntityContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class InterfaceTagTransformer implements EntityAPITransformer<InterfaceTagMO, InterfaceTagWrapper> {

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

    @NotNull
    @Override
    public InterfaceTagMO convertToMO(@NotNull EntityContainer<InterfaceTagWrapper> interfaceTagWrapperEntityContainer) {
        return convertToMO(interfaceTagWrapperEntityContainer.getEntity());
    }

    @NotNull
    @Override
    public InterfaceTagMO convertToMO(@NotNull InterfaceTagWrapper e) {
        return factory.internalAsResource(e);
    }

    @NotNull
    @Override
    public EntityContainer<InterfaceTagWrapper> convertFromMO(@NotNull InterfaceTagMO interfaceTagMO) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(interfaceTagMO,true);
    }

    @NotNull
    @Override
    public EntityContainer<InterfaceTagWrapper> convertFromMO(@NotNull InterfaceTagMO m, boolean strict) throws ResourceFactory.InvalidResourceException {
        return new EntityContainer<>(InterfaceTagWrapper.fromInterfaceTag(factory.internalFromResource(m).left));
    }

    @NotNull
    @Override
    public Item<InterfaceTagMO> convertToItem(@NotNull InterfaceTagMO m) {
        return new ItemBuilder<InterfaceTagMO>(m.getName(), m.getId(), getResourceType())
                .setContent(m)
                .build();
    }
}
