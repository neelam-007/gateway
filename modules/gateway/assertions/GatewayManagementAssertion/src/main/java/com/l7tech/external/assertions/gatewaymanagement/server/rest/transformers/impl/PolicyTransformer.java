package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.policy.Policy;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PolicyTransformer extends APIResourceWsmanBaseTransformer<PolicyMO, Policy, PolicyResourceFactory> {

    @Override
    @Inject
    protected void setFactory(PolicyResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<PolicyMO> convertToItem(PolicyMO m) {
        return new ItemBuilder<PolicyMO>(m.getPolicyDetail().getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
