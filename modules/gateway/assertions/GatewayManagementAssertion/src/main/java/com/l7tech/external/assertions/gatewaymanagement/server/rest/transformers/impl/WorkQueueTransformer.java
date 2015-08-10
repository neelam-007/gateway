package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.WorkQueueMO;
import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.bundling.EntityContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class WorkQueueTransformer extends EntityManagerAPITransformer<WorkQueueMO, WorkQueue> implements EntityAPITransformer<WorkQueueMO, WorkQueue> {

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.WORK_QUEUE.toString();
    }

    @NotNull
    @Override
    public WorkQueueMO convertToMO(@NotNull EntityContainer<WorkQueue> userEntityContainer, SecretsEncryptor secretsEncryptor) {
        return convertToMO(userEntityContainer.getEntity(), secretsEncryptor);
    }


    @NotNull
    public WorkQueueMO convertToMO(@NotNull WorkQueue workQueue) {
        return convertToMO(workQueue, null);
    }

    @NotNull
    @Override
    public WorkQueueMO convertToMO(@NotNull WorkQueue workQueue, SecretsEncryptor secretsEncryptor) {
        WorkQueueMO workQueueMO = ManagedObjectFactory.createWorkQueueMO();
        workQueueMO.setId(workQueue.getId());
        workQueueMO.setVersion(workQueue.getVersion());
        workQueueMO.setName(workQueue.getName());
        workQueueMO.setMaxQueueSize(workQueue.getMaxQueueSize());
        workQueueMO.setThreadPoolMax(workQueue.getThreadPoolMax());
        workQueueMO.setRejectPolicy(workQueue.getRejectPolicy());
        doSecurityZoneToMO(workQueueMO, workQueue);
        return workQueueMO;
    }

    @NotNull
    @Override
    public EntityContainer<WorkQueue> convertFromMO(@NotNull WorkQueueMO workQueueMO, SecretsEncryptor secretsEncryptor)
            throws ResourceFactory.InvalidResourceException {
        return convertFromMO(workQueueMO, true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<WorkQueue> convertFromMO(@NotNull WorkQueueMO workQueueMO, boolean strict, SecretsEncryptor secretsEncryptor)
            throws ResourceFactory.InvalidResourceException {
        WorkQueue workQueue = new WorkQueue();
        workQueue.setId(workQueueMO.getId());
        if (workQueueMO.getVersion() != null) {
            workQueue.setVersion(workQueueMO.getVersion());
        }
        workQueue.setName(workQueueMO.getName());
        workQueue.setMaxQueueSize(workQueueMO.getMaxQueueSize());
        workQueue.setThreadPoolMax(workQueueMO.getThreadPoolMax());
        final String rejectPolicy = workQueueMO.getRejectPolicy();
        if (WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY.equals(rejectPolicy) || WorkQueue.REJECT_POLICY_WAIT_FOR_ROOM.equals(rejectPolicy)) {
            workQueue.setRejectPolicy(rejectPolicy);
        }
        else {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                    "Unknown reject policy '" + rejectPolicy + "'.");
        }
        doSecurityZoneFromMO(workQueueMO, workQueue, strict);

        return new EntityContainer<>(workQueue);
    }

    @NotNull
    @Override
    public Item<WorkQueueMO> convertToItem(@NotNull WorkQueueMO m) {
        return new ItemBuilder<WorkQueueMO>(m.getName(), m.getId(), EntityType.WORK_QUEUE.name())
                .setContent(m)
                .build();
    }
}
