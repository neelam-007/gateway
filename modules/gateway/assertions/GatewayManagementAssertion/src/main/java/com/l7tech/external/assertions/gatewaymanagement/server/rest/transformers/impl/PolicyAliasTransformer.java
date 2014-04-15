package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.PolicyAliasMO;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.server.policy.PolicyManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PolicyAliasTransformer extends APIResourceWsmanBaseTransformer<PolicyAliasMO, PolicyAlias, AliasHeader<Policy>, PolicyAliasResourceFactory> {

    @Inject
    private PolicyManager policyManager;

    @Override
    @Inject
    protected void setFactory(PolicyAliasResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<PolicyAliasMO> convertToItem(PolicyAliasMO m) {
        Item<PolicyAliasMO> item = new ItemBuilder<PolicyAliasMO>(m.getPolicyReference().getId(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
        try {
            Policy policy = policyManager.findByPrimaryKey(Goid.parseGoid(m.getPolicyReference().getId()));
            if(policy != null) {
                item.setName(policy.getName() + " alias");
            }
        } catch (Throwable t) {
            //do nothing.
        }
        return item;
    }

    @Override
    public Item<PolicyAliasMO> convertToItem(EntityHeader header) {
        if (header instanceof AliasHeader) {
            return new ItemBuilder<PolicyAliasMO>(((AliasHeader) header).getAliasedEntityId().toString(), header.getStrId(), factory.getType().name())
                    .build();
        } else {
            return super.convertToItem(header);
        }
    }
}
