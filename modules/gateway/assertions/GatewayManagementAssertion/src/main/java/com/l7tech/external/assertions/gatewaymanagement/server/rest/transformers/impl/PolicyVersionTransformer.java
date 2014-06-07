package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyVersionMO;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.policy.PolicyManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PolicyVersionTransformer implements EntityAPITransformer<PolicyVersionMO, PolicyVersion> {

    @Inject
    private PolicyManager policyManager;

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.POLICY_VERSION.toString();
    }

    @NotNull
    @Override
    public PolicyVersionMO convertToMO(@NotNull EntityContainer<PolicyVersion> policyVersionEntityContainer) {
        return convertToMO(policyVersionEntityContainer.getEntity());
    }

    @NotNull
    @Override
    public PolicyVersionMO convertToMO(@NotNull PolicyVersion policyVersion) {
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

    @NotNull
    @Override
    public EntityContainer<PolicyVersion> convertFromMO(@NotNull PolicyVersionMO policyVersionMO) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(policyVersionMO,true);
    }

    @NotNull
    @Override
    public EntityContainer<PolicyVersion> convertFromMO(@NotNull PolicyVersionMO policyVersionMO, boolean strict) throws ResourceFactory.InvalidResourceException {

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
        return new EntityContainer<>(policyVersion);
    }

    @NotNull
    @Override
    public Item<PolicyVersionMO> convertToItem(@NotNull PolicyVersionMO m) {
        return new ItemBuilder<PolicyVersionMO>("Policy Version: " + m.getOrdinal(), m.getId(), EntityType.POLICY_VERSION.name())
                .setContent(m)
                .build();
    }
}
