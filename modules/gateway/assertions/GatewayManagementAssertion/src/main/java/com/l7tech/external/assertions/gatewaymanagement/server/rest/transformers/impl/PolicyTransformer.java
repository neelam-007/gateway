package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.server.bundling.PersistentEntityContainer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PolicyTransformer extends APIResourceWsmanBaseTransformer<PolicyMO, Policy, PolicyHeader, PolicyResourceFactory> {

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

    @Override
    public PersistentEntityContainer<Policy> convertFromMO(PolicyMO policyMO,boolean strict) throws ResourceFactory.InvalidResourceException {
        PersistentEntityContainer<Policy> entityBag = super.convertFromMO(policyMO,strict);
        //preserve the policy guid if it is set.
        if(policyMO.getGuid() != null) {
            entityBag.getEntity().setGuid(policyMO.getGuid());
        }
        //need to update the policy version as wsman does not set it. SSG-8476
        if(policyMO.getVersion() != null) {
            entityBag.getEntity().setVersion(policyMO.getVersion());
        }
        return entityBag;
    }
}
