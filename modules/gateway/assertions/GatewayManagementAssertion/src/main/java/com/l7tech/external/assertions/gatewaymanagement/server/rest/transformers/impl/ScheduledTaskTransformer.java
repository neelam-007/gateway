package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;

@Component
public class ScheduledTaskTransformer implements EntityAPITransformer<ScheduledTaskMO, ScheduledTask> {

    @Inject
    PolicyManager policyManager;

    @Inject
    SecurityZoneManager securityZoneManager;

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.SCHEDULED_TASK.toString();
    }

    @NotNull
    @Override
    public ScheduledTaskMO convertToMO(@NotNull EntityContainer<ScheduledTask> userEntityContainer,  SecretsEncryptor secretsEncryptor) {
        return convertToMO(userEntityContainer.getEntity(), secretsEncryptor);
    }


    @NotNull
    public ScheduledTaskMO convertToMO(@NotNull ScheduledTask scheduledTask) {
        return convertToMO(scheduledTask, null);
    }

    @NotNull
    @Override
    public ScheduledTaskMO convertToMO(@NotNull ScheduledTask scheduledTask,  SecretsEncryptor secretsEncryptor) {
        ScheduledTaskMO scheduledTaskMO = ManagedObjectFactory.createScheduledTaskMO();
        scheduledTaskMO.setId(scheduledTask.getId());
        scheduledTaskMO.setVersion(scheduledTask.getVersion());
        scheduledTaskMO.setName(scheduledTask.getName());
        scheduledTaskMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, scheduledTask.getPolicy().getId()));
        scheduledTaskMO.setUseOneNode(scheduledTask.isUseOneNode());
        switch (scheduledTask.getJobType()) {
            case ONE_TIME:
                scheduledTaskMO.setJobType(ScheduledTaskMO.ScheduledTaskJobType.ONE_TIME);
                break;
            case RECURRING:
                scheduledTaskMO.setJobType(ScheduledTaskMO.ScheduledTaskJobType.RECURRING);
                break;
            default:
                throw new ResourceFactory.ResourceAccessException("Unknown job type '" + scheduledTask.getJobType() + "'.");
        }
        switch (scheduledTask.getJobStatus()) {
            case SCHEDULED:
                scheduledTaskMO.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.SCHEDULED);
                break;
            case COMPLETED:
                scheduledTaskMO.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.COMPLETED);
                break;
            case DISABLED:
                scheduledTaskMO.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.DISABLED);
                break;
            default:
                throw new ResourceFactory.ResourceAccessException("Unknown job status '" + scheduledTask.getJobStatus() + "'.");
        }
        scheduledTaskMO.setExecutionDate(scheduledTask.getExecutionDate() == 0 ? null: new Date(scheduledTask.getExecutionDate()));
        scheduledTaskMO.setCronExpression(scheduledTask.getCronExpression());
        scheduledTaskMO.setProperties(scheduledTask.getProperties());
        doSecurityZoneToMO(scheduledTaskMO, scheduledTask);

        return scheduledTaskMO;
    }

    @NotNull
    @Override
    public EntityContainer<ScheduledTask> convertFromMO(@NotNull ScheduledTaskMO scheduledTaskMO, SecretsEncryptor secretsEncryptor)
            throws ResourceFactory.InvalidResourceException {
        return convertFromMO(scheduledTaskMO, true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<ScheduledTask> convertFromMO(@NotNull ScheduledTaskMO scheduledTaskMO, boolean strict, SecretsEncryptor secretsEncryptor)
            throws ResourceFactory.InvalidResourceException {
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setId(scheduledTaskMO.getId());
        if (scheduledTaskMO.getVersion() != null) {
            scheduledTask.setVersion(scheduledTaskMO.getVersion());
        }
        scheduledTask.setName(scheduledTaskMO.getName());
        try {
            scheduledTask.setPolicy(policyManager.findByPrimaryKey(Goid.parseGoid(scheduledTaskMO.getPolicyReference().getId()))) ;
            if(strict){
                if(!(PolicyType.POLICY_BACKED_OPERATION.equals(scheduledTask.getPolicy().getType()) &&
                        "com.l7tech.objectmodel.polback.BackgroundTask".equals(scheduledTask.getPolicy().getInternalTag())
                        && "run".equals(scheduledTask.getPolicy().getInternalSubTag()))) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Policy referenced must be tagged Background task");
                }
            }
        } catch (FindException | IllegalArgumentException e) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid or unknown policy reference '" + scheduledTaskMO.getPolicyReference().getId() + "'.");
        }
        scheduledTask.setUseOneNode(scheduledTaskMO.isUseOneNode());
        switch(scheduledTaskMO.getJobType()){
            case ONE_TIME:
                scheduledTask.setJobType(JobType.ONE_TIME);
                break;
            case RECURRING:
                scheduledTask.setJobType(JobType.RECURRING);
                break;
            default:
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Unknown job type '" + scheduledTaskMO.getJobType() + "'.");
        }
        switch(scheduledTaskMO.getJobStatus()){
            case SCHEDULED:
                scheduledTask.setJobStatus(JobStatus.SCHEDULED);
                break;
            case COMPLETED:
                scheduledTask.setJobStatus(JobStatus.COMPLETED);
                break;
            case DISABLED:
                scheduledTask.setJobStatus(JobStatus.DISABLED);
                break;
            default:
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Unknown job status '" + scheduledTaskMO.getJobStatus() + "'.");
        }
        if(scheduledTaskMO.getExecutionDate()!=null) {
            scheduledTask.setExecutionDate(scheduledTaskMO.getExecutionDate().getTime());
        }
        scheduledTask.setCronExpression(scheduledTaskMO.getCronExpression());
        scheduledTask.setProperties(scheduledTaskMO.getProperties());
        doSecurityZoneFromMO(scheduledTaskMO, scheduledTask, strict);

        return new EntityContainer<>(scheduledTask);
    }

    protected void doSecurityZoneToMO(ScheduledTaskMO resource, final ScheduledTask entity) {
        if (entity instanceof ZoneableEntity) {

            if (entity.getSecurityZone() != null) {
                resource.setSecurityZoneId(entity.getSecurityZone().getId());
                resource.setSecurityZone(entity.getSecurityZone().getName());
            }
        }
    }

    protected void doSecurityZoneFromMO(ScheduledTaskMO resource, final ScheduledTask entity, final boolean strict)
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
    public Item<ScheduledTaskMO> convertToItem(@NotNull ScheduledTaskMO m) {
        return new ItemBuilder<ScheduledTaskMO>(m.getName(), m.getId(), EntityType.SCHEDULED_TASK.name())
                .setContent(m)
                .build();
    }
}
