package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EncapsulatedAssertionResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class EncapsulatedAssertionTransformer extends APIResourceWsmanBaseTransformer<EncapsulatedAssertionMO, EncapsulatedAssertionConfig, EncapsulatedAssertionResourceFactory> {

    @Override
    @Inject
    protected void setFactory(EncapsulatedAssertionResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<EncapsulatedAssertionMO> convertToItem(EncapsulatedAssertionMO m) {
        return new ItemBuilder<EncapsulatedAssertionMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
