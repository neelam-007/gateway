package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.IdentityProviderResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.IdentityProviderMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class IdentityProviderTransformer extends APIResourceWsmanBaseTransformer<IdentityProviderMO, IdentityProviderConfig,EntityHeader, IdentityProviderResourceFactory> {

    @Override
    @Inject
    protected void setFactory(IdentityProviderResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<IdentityProviderMO> convertToItem(IdentityProviderMO m) {
        return new ItemBuilder<IdentityProviderMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
