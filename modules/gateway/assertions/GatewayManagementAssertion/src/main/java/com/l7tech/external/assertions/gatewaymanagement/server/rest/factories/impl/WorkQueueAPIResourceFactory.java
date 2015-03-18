package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.WorkQueueTransformer;
import com.l7tech.gateway.api.WorkQueueMO;
import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.workqueue.WorkQueueEntityManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class WorkQueueAPIResourceFactory extends
        EntityManagerAPIResourceFactory<WorkQueueMO, WorkQueue, EntityHeader> {

    @Inject
    private WorkQueueTransformer transformer;
    @Inject
    private WorkQueueEntityManager workQueueEntityManager;

    @NotNull
    @Override
    public EntityType getResourceEntityType() {
        return EntityType.WORK_QUEUE;
    }

    @Override
    protected WorkQueue convertFromMO(WorkQueueMO resource) throws ResourceFactory.InvalidResourceException {
        return transformer.convertFromMO(resource, null).getEntity();
    }

    @Override
    protected WorkQueueMO convertToMO(WorkQueue entity) {
        return transformer.convertToMO(entity);
    }

    @Override
    protected WorkQueueEntityManager getEntityManager() {
        return workQueueEntityManager;
    }
}
