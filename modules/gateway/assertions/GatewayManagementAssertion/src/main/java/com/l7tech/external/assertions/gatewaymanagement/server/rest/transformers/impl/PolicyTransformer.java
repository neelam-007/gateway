package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.server.bundling.EntityContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PolicyTransformer extends APIResourceWsmanBaseTransformer<PolicyMO, Policy, PolicyHeader, PolicyResourceFactory> {

    @Override
    @Inject
    protected void setFactory(PolicyResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<PolicyMO> convertToItem(@NotNull PolicyMO m) {
        return new ItemBuilder<PolicyMO>(m.getPolicyDetail().getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @NotNull
    @Override
    public EntityContainer<Policy> convertFromMO(@NotNull PolicyMO policyMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        EntityContainer<Policy> entityBag = super.convertFromMO(policyMO,strict, secretsEncryptor);
        //preserve the policy guid if it is set.
        if(policyMO.getGuid() != null) {
            entityBag.getEntity().setGuid(policyMO.getGuid());
        }
        return entityBag;
    }
}
