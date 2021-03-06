package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.RevocationCheckingPolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.RevocationCheckingPolicyMO;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class RevocationCheckingPolicyTransformer extends APIResourceWsmanBaseTransformer<RevocationCheckingPolicyMO, RevocationCheckPolicy,EntityHeader, RevocationCheckingPolicyResourceFactory> {

    @Override
    @Inject
    @Named("revocationCheckingPolicyResourceFactory")
    protected void setFactory(RevocationCheckingPolicyResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<RevocationCheckingPolicyMO> convertToItem(@NotNull RevocationCheckingPolicyMO m) {
        return new ItemBuilder<RevocationCheckingPolicyMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
