package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.AssertionSecurityZoneResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.AssertionSecurityZoneMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.AssertionAccess;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class AssertionSecurityZoneTransformer extends APIResourceWsmanBaseTransformer<AssertionSecurityZoneMO, AssertionAccess,EntityHeader,  AssertionSecurityZoneResourceFactory> {

    @Override
    @Inject
    protected void setFactory(AssertionSecurityZoneResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<AssertionSecurityZoneMO> convertToItem(@NotNull AssertionSecurityZoneMO m) {
        return new ItemBuilder<AssertionSecurityZoneMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
