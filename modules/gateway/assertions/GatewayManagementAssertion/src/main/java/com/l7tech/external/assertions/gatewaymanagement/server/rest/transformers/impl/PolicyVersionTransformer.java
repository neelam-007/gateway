package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyVersionMO;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.policy.PolicyManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PolicyVersionTransformer implements APITransformer<PolicyVersionMO, PolicyVersion> {

    @Inject
    private PolicyManager policyManager;

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.POLICY_VERSION.toString();
    }

    @Override
    public PolicyVersionMO convertToMO(PolicyVersion policyVersion) {
        PolicyVersionMO policyVersionMO = ManagedObjectFactory.createPolicyVersionMO();
        policyVersionMO.setActive(policyVersion.isActive());
        policyVersionMO.setComment(policyVersion.getName());
        policyVersionMO.setId(policyVersion.getId());
        policyVersionMO.setPolicyId(policyVersion.getPolicyGoid().toString());
        policyVersionMO.setTime(policyVersion.getTime());
        policyVersionMO.setOrdinal(policyVersion.getOrdinal());
        policyVersionMO.setXml(policyVersion.getXml());
        return policyVersionMO;
    }

    @Override
    public PolicyVersion convertFromMO(PolicyVersionMO policyVersionMO) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(policyVersionMO, true);
    }

    @Override
    public PolicyVersion convertFromMO(PolicyVersionMO policyVersionMO, boolean strict) throws ResourceFactory.InvalidResourceException {
        PolicyVersion policyVersion = new PolicyVersion();
        policyVersion.setActive(policyVersionMO.isActive());
        policyVersion.setName(policyVersionMO.getComment());
        policyVersion.setGoid(Goid.parseGoid(policyVersionMO.getId()));
        Goid policyGoid = Goid.parseGoid(policyVersionMO.getPolicyId());
        try {
            policyManager.findByPrimaryKey(policyGoid);
        } catch (FindException e) {
            if(strict) {
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "Cannot find policy with id: "+e.getMessage());
            }
        }
        policyVersion.setPolicyGoid(policyGoid);
        policyVersion.setTime(policyVersionMO.getTime());
        policyVersion.setOrdinal(policyVersionMO.getOrdinal());
        policyVersion.setXml(policyVersionMO.getXml());
        return policyVersion;
    }

    @Override
    public Item<PolicyVersionMO> convertToItem(PolicyVersionMO m) {
        return new ItemBuilder<PolicyVersionMO>("Policy Version: " + m.getVersion(), m.getId(), EntityType.POLICY_VERSION.name())
                .setContent(m)
                .build();
    }

    @Override
    public Item<PolicyVersionMO> convertToItem(EntityHeader header) {
        return new ItemBuilder<PolicyVersionMO>("Policy Version: " + header.getVersion(), header.getStrId(), EntityType.POLICY_VERSION.name())
                .build();
    }
}
