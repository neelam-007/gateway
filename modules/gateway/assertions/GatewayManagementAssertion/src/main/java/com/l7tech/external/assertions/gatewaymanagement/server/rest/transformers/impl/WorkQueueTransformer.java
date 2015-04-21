package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.*;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class WorkQueueTransformer implements EntityAPITransformer<WorkQueueMO, WorkQueue> {

    @Inject
    ServiceManager serviceManager;

    @Inject
    SecurityZoneManager securityZoneManager;

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

    protected void doSecurityZoneToMO(WorkQueueMO resource, final WorkQueue entity) {
        if (entity instanceof ZoneableEntity) {

            if (entity.getSecurityZone() != null) {
                resource.setSecurityZoneId(entity.getSecurityZone().getId());
                resource.setSecurityZone(entity.getSecurityZone().getName());
            }
        }
    }

    protected void doSecurityZoneFromMO(WorkQueueMO resource, final WorkQueue entity, final boolean strict)
            throws ResourceFactory.InvalidResourceException {
        if (entity instanceof ZoneableEntity) {

            if (resource.getSecurityZoneId() != null && !resource.getSecurityZoneId().isEmpty()) {
                final Goid securityZoneId;
                try {
                    securityZoneId = GoidUpgradeMapper.mapId(EntityType.SECURITY_ZONE, resource.getSecurityZoneId());
                } catch (IllegalArgumentException nfe) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                            "invalid or unknown security zone reference");
                }
                SecurityZone zone = null;
                try {
                    zone = securityZoneManager.findByPrimaryKey(securityZoneId);
                } catch (FindException e) {
                    if (strict)
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                                "invalid or unknown security zone reference");
                }
                if (strict) {
                    if (zone == null)
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                                "invalid or unknown security zone reference");
                    if (!zone.permitsEntityType(EntityType.findTypeByEntity(entity.getClass())))
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                                "entity type not permitted for referenced security zone");
                } else if (zone == null) {
                    zone = new SecurityZone();
                    zone.setGoid(securityZoneId);
                    zone.setName(resource.getSecurityZone());
                }
                entity.setSecurityZone(zone);
            }
        }
    }

    @NotNull
    @Override
    public Item<WorkQueueMO> convertToItem(@NotNull WorkQueueMO m) {
        return new ItemBuilder<WorkQueueMO>(m.getName(), m.getId(), EntityType.WORK_QUEUE.name())
                .setContent(m)
                .build();
    }
}
