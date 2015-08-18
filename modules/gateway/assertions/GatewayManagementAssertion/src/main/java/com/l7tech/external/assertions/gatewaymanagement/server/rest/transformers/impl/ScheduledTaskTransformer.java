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
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;

@Component
public class ScheduledTaskTransformer extends EntityManagerAPITransformer<ScheduledTaskMO, ScheduledTask> implements EntityAPITransformer<ScheduledTaskMO, ScheduledTask> {

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
        scheduledTaskMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, scheduledTask.getPolicyGoid().toString()));
        scheduledTaskMO.setUseOneNode(scheduledTask.isUseOneNode());
        scheduledTaskMO.setExecuteOnCreate(scheduledTask.isExecuteOnCreate());
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

        scheduledTask.setPolicyGoid(Goid.parseGoid(scheduledTaskMO.getPolicyReference().getId()));
        if(strict){
            try {
                Policy refPolicy = policyManager.findByPrimaryKey(scheduledTask.getPolicyGoid());
                if(refPolicy == null) {
                    throwPolicyNotFound(scheduledTaskMO);
                }
                if(!(PolicyType.POLICY_BACKED_OPERATION.equals(refPolicy.getType()) &&
                        "com.l7tech.objectmodel.polback.BackgroundTask".equals(refPolicy.getInternalTag())
                        && "run".equals(refPolicy.getInternalSubTag()))) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Policy referenced must be tagged Background task");
                }
            } catch (FindException | IllegalArgumentException e) {
                return throwPolicyNotFound(scheduledTaskMO);
            }
        }

        scheduledTask.setUseOneNode(scheduledTaskMO.isUseOneNode());
        scheduledTask.setExecuteOnCreate(scheduledTaskMO.isExecuteOnCreate() == null ? false : scheduledTaskMO.isExecuteOnCreate());
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

    private EntityContainer<ScheduledTask> throwPolicyNotFound(ScheduledTaskMO scheduledTaskMO) throws ResourceFactory.InvalidResourceException {
        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid or unknown policy reference '" + scheduledTaskMO.getPolicyReference().getId() + "'.");
    }

    @NotNull
    @Override
    public Item<ScheduledTaskMO> convertToItem(@NotNull ScheduledTaskMO m) {
        return new ItemBuilder<ScheduledTaskMO>(m.getName(), m.getId(), EntityType.SCHEDULED_TASK.name())
                .setContent(m)
                .build();
    }
}
