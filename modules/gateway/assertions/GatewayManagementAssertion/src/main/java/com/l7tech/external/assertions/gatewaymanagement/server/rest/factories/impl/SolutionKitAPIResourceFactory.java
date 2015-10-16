package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.SolutionKitTransformer;
import com.l7tech.gateway.api.SolutionKitMO;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.solutionkit.SolutionKitManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 */
@Component
public class SolutionKitAPIResourceFactory extends EntityManagerAPIResourceFactory<SolutionKitMO, SolutionKit, SolutionKitHeader> {
    @Inject
    private SolutionKitTransformer transformer;
    @Inject
    private SolutionKitManager manager;

    @Override
    protected EntityType getResourceEntityType() {
        return EntityType.SOLUTION_KIT;
    }

    @Override
    protected SolutionKitManager getEntityManager() {
        return manager;
    }

    @Override
    protected SolutionKit convertFromMO(final SolutionKitMO resource) throws ResourceFactory.InvalidResourceException {
        return transformer.convertFromMO(resource, null).getEntity();
    }

    @Override
    protected SolutionKitMO convertToMO(final SolutionKit entity) {
        return transformer.convertToMO(entity);
    }
}
