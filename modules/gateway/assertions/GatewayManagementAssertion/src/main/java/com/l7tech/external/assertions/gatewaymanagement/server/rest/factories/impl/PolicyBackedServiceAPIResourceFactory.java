package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyBackedServiceTransformer;
import com.l7tech.gateway.api.PolicyBackedServiceMO;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.server.polback.PolicyBackedServiceManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PolicyBackedServiceAPIResourceFactory extends
        EntityManagerAPIResourceFactory<PolicyBackedServiceMO, PolicyBackedService, EntityHeader> {

    @Inject
    private PolicyBackedServiceTransformer transformer;
    @Inject
    private PolicyBackedServiceManager policyBackedServiceManager;

    @NotNull
    @Override
    public EntityType getResourceEntityType() {
        return EntityType.POLICY_BACKED_SERVICE;
    }

    @Override
    protected PolicyBackedService convertFromMO(PolicyBackedServiceMO resource) throws ResourceFactory.InvalidResourceException {
        return transformer.convertFromMO(resource, null).getEntity();
    }

    @Override
    protected PolicyBackedServiceMO convertToMO(PolicyBackedService entity) {
        return transformer.convertToMO(entity, null);
    }

    @Override
    protected PolicyBackedServiceManager getEntityManager() {
        return policyBackedServiceManager;
    }
}
