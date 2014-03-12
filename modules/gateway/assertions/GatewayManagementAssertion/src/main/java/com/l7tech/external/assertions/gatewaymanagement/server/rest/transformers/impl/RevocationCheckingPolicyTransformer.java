package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.RevocationCheckingPolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.RevocationCheckingPolicyMO;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.EntityHeader;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class RevocationCheckingPolicyTransformer extends APIResourceWsmanBaseTransformer<RevocationCheckingPolicyMO, RevocationCheckPolicy,EntityHeader, RevocationCheckingPolicyResourceFactory> {

    @Override
    @Inject
    protected void setFactory(RevocationCheckingPolicyResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<RevocationCheckingPolicyMO> convertToItem(RevocationCheckingPolicyMO m) {
        return new ItemBuilder<RevocationCheckingPolicyMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
