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
import com.l7tech.util.MasterPasswordManager;
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
    public PrivateKeyMO convertToMO(@NotNull EntityContainer<SsgKeyEntry> ssgKeyEntryEntityContainer,  MasterPasswordManager passwordManager) {
        return convertToMO(ssgKeyEntryEntityContainer.getEntity(), passwordManager);
    }

    @NotNull
    public PrivateKeyMO convertToMO(@NotNull SsgKeyEntry e) {
        return convertToMO(e,null);
    }

    @NotNull
    @Override
    public PrivateKeyMO convertToMO(@NotNull SsgKeyEntry e,  MasterPasswordManager passwordManager) {
        //need to 'identify' the MO because by default the wsman factories will no set the id and version in the
        // asResource method
        PrivateKeyMO mo =  factory.identify(factory.asResource(e), e);

        if(passwordManager!=null){
            // todo encrypt and attach key info
        }

        return mo;
    }

    @NotNull
    @Override
    public EntityContainer<SsgKeyEntry> convertFromMO(@NotNull PrivateKeyMO m, MasterPasswordManager passwordManager) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(m,true, passwordManager);
    }

    @NotNull
    @Override
    public EntityContainer<SsgKeyEntry> convertFromMO(@NotNull PrivateKeyMO m, boolean strict, MasterPasswordManager passwordManager) throws ResourceFactory.InvalidResourceException {
        return new EntityContainer<>(new SsgKeyEntry(Goid.parseGoid(m.getKeystoreId()),m.getAlias(),null,null));
    }
}
