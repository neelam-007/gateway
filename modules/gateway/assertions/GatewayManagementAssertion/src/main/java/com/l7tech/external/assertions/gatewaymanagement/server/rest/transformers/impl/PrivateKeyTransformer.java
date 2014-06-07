package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PrivateKeyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.bundling.EntityContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PrivateKeyTransformer implements EntityAPITransformer<PrivateKeyMO, SsgKeyEntry> {

    @Inject
    protected PrivateKeyResourceFactory factory;

    @NotNull
    @Override
    public Item<PrivateKeyMO> convertToItem(@NotNull PrivateKeyMO m) {
        return new ItemBuilder<PrivateKeyMO>(m.getAlias(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @Override
    @NotNull
    public String getResourceType(){
        return factory.getType().toString();
    }

    @NotNull
    @Override
    public PrivateKeyMO convertToMO(@NotNull EntityContainer<SsgKeyEntry> ssgKeyEntryEntityContainer) {
        return convertToMO(ssgKeyEntryEntityContainer.getEntity());
    }

    @NotNull
    @Override
    public PrivateKeyMO convertToMO(@NotNull SsgKeyEntry e) {
        //need to 'identify' the MO because by default the wsman factories will no set the id and version in the
        // asResource method
        return factory.identify(factory.asResource(e), e);
    }

    @NotNull
    @Override
    public EntityContainer<SsgKeyEntry> convertFromMO(@NotNull PrivateKeyMO m) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(m,true);
    }

    @NotNull
    @Override
    public EntityContainer<SsgKeyEntry> convertFromMO(@NotNull PrivateKeyMO m, boolean strict) throws ResourceFactory.InvalidResourceException {
        return new EntityContainer<>(new SsgKeyEntry(Goid.parseGoid(m.getKeystoreId()),m.getAlias(),null,null));
    }
}
