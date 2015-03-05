package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.InterfaceTagResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.bundling.InterfaceTagContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class InterfaceTagTransformer implements EntityAPITransformer<InterfaceTagMO, InterfaceTag> {

    /**
     * The wiseman resource factory
     */
    protected InterfaceTagResourceFactory factory;

    @Inject
    @Named("interfaceTagResourceFactory")
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
    public InterfaceTagMO convertToMO(@NotNull EntityContainer<InterfaceTag> interfaceTagContainer,  SecretsEncryptor secretsEncryptor) {
        return convertToMO(interfaceTagContainer.getEntity(), secretsEncryptor);
    }

    @NotNull
    public InterfaceTagMO convertToMO(@NotNull InterfaceTag e) {
        return convertToMO(e,null);
    }

    @NotNull
    @Override
    public InterfaceTagMO convertToMO(@NotNull InterfaceTag e,  SecretsEncryptor secretsEncryptor) {
        return factory.internalAsResource(e);
    }

    @NotNull
    @Override
    public EntityContainer<InterfaceTag> convertFromMO(@NotNull InterfaceTagMO interfaceTagMO, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(interfaceTagMO,true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<InterfaceTag> convertFromMO(@NotNull InterfaceTagMO m, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return new InterfaceTagContainer(factory.internalFromResource(m).left);
    }

    @NotNull
    @Override
    public Item<InterfaceTagMO> convertToItem(@NotNull InterfaceTagMO m) {
        return new ItemBuilder<InterfaceTagMO>(m.getName(), m.getId(), getResourceType())
                .setContent(m)
                .build();
    }
}
