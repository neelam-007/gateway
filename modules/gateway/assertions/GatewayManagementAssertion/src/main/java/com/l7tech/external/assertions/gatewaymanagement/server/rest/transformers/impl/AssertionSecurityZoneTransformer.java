package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.AssertionSecurityZoneResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.AssertionSecurityZoneMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.policy.AssertionAccess;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class AssertionSecurityZoneTransformer extends APIResourceWsmanBaseTransformer<AssertionSecurityZoneMO, AssertionAccess, AssertionSecurityZoneResourceFactory> {

    @Override
    @Inject
    protected void setFactory(AssertionSecurityZoneResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<AssertionSecurityZoneMO> convertToItem(AssertionSecurityZoneMO m) {
        return new ItemBuilder<AssertionSecurityZoneMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
