package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.PolicyAliasMO;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.server.policy.PolicyManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class PolicyAliasTransformer extends APIResourceWsmanBaseTransformer<PolicyAliasMO, PolicyAlias, AliasHeader<Policy>, PolicyAliasResourceFactory> {

    @Inject
    private PolicyManager policyManager;

    @Override
    @Inject
    @Named("policyAliasResourceFactory")
    protected void setFactory(PolicyAliasResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<PolicyAliasMO> convertToItem(@NotNull PolicyAliasMO m) {
        return new ItemBuilder<PolicyAliasMO>(findPolicyAliasName(Goid.parseGoid(m.getPolicyReference().getId())), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    /**
     * Finds the policy alias name by looking for the policy with the given id. If this policy cannot be found the policy
     * id is returned.
     *
     * @param policyID The id of the policy to search for
     * @return The name of the policy alias
     */
    private String findPolicyAliasName(Goid policyID) {
        try {
            Policy policy = policyManager.findByPrimaryKey(policyID);
            if (policy != null) {
                return policy.getName() + " alias";
            }
        } catch (Throwable t) {
            //we do not want to throw here and default to using the id if a policy cannot be found
        }
        return policyID.toString();
    }
}
