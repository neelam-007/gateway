package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.GenericEntityResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class GenericEntityTransformer extends APIResourceWsmanBaseTransformer<GenericEntityMO, GenericEntity, GenericEntityHeader,GenericEntityResourceFactory> {

    @Override
    @Inject
    @Named("genericEntityResourceFactory")
    protected void setFactory(GenericEntityResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<GenericEntityMO> convertToItem(@NotNull GenericEntityMO m) {
        return new ItemBuilder<GenericEntityMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
