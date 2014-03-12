package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.GenericEntityResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class GenericEntityTransformer extends APIResourceWsmanBaseTransformer<GenericEntityMO, GenericEntity, GenericEntityHeader,GenericEntityResourceFactory> {

    @Override
    @Inject
    protected void setFactory(GenericEntityResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<GenericEntityMO> convertToItem(GenericEntityMO m) {
        return new ItemBuilder<GenericEntityMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
