package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ScheduledTaskTransformer;
import com.l7tech.gateway.api.ScheduledTaskMO;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.task.ScheduledTaskManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class ScheduledTaskAPIResourceFactory extends
        EntityManagerAPIResourceFactory<ScheduledTaskMO, ScheduledTask, EntityHeader> {

    @Inject
    private ScheduledTaskTransformer transformer;
    @Inject
    private ScheduledTaskManager scheduledTaskManager;

    @NotNull
    @Override
    public EntityType getResourceEntityType() {
        return EntityType.SCHEDULED_TASK;
    }

    @Override
    protected ScheduledTask convertFromMO(ScheduledTaskMO resource) throws ResourceFactory.InvalidResourceException {
        return transformer.convertFromMO(resource, null).getEntity();
    }

    @Override
    protected ScheduledTaskMO convertToMO(ScheduledTask entity) {
        return transformer.convertToMO(entity);
    }

    @Override
    protected ScheduledTaskManager getEntityManager() {
        return scheduledTaskManager;
    }
}
