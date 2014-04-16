package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PrivateKeyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.bundling.PrivateKeyContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PrivateKeyTransformer implements APITransformer<PrivateKeyMO, SsgKeyEntry> {

    @Inject
    protected PrivateKeyResourceFactory factory;

    @Override
    public Item<PrivateKeyMO> convertToItem(PrivateKeyMO m) {
        return new ItemBuilder<PrivateKeyMO>(m.getAlias(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @Override
    @NotNull
    public String getResourceType(){
        return factory.getType().toString();
    }

    @Override
    public PrivateKeyMO convertToMO(SsgKeyEntry e) {
        //need to 'identify' the MO because by default the wsman factories will no set the id and version in the
        // asResource method
        return factory.identify(factory.asResource(e), e);
    }

    @Override
    public EntityContainer<SsgKeyEntry> convertFromMO(PrivateKeyMO m) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(m,true);
    }

    @Override
    public EntityContainer<SsgKeyEntry> convertFromMO(PrivateKeyMO m, boolean strict) throws ResourceFactory.InvalidResourceException {
        return new PrivateKeyContainer(new SsgKeyEntry(Goid.parseGoid(m.getKeystoreId()),m.getAlias(),null,null));
    }
}
