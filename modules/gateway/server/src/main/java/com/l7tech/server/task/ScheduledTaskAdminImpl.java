package com.l7tech.server.task;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.gateway.common.task.ScheduledTaskAdmin;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Created by luiwy01 on 3/9/2015.
 */
public class ScheduledTaskAdminImpl implements ScheduledTaskAdmin {

    @Inject
    private ScheduledTaskManager scheduledTaskManager;

    @Inject
    private RbacServices rbacServices;

    @Override
    public ScheduledTask getScheduledTask(Goid id) throws FindException {
        return scheduledTaskManager.findByPrimaryKey(id);
    }

    @Override
    public Collection<ScheduledTask> getAllScheduledTasks() throws FindException {
        return scheduledTaskManager.findAll();
    }

    @Override
    public Goid saveScheduledTask(ScheduledTask scheduledTask) throws UpdateException, SaveException {

        boolean isCreate = scheduledTask.getGoid() == null || Goid.isDefault(scheduledTask.getGoid());
        // only users that have role management access can set a user for a scheduled task
        try {
            if (scheduledTask.getIdProviderGoid() != null || scheduledTask.getUserId() != null) {
                validatePermitted(EntityType.USER, OperationType.UPDATE);
                validatePermitted(EntityType.USER, OperationType.CREATE);
            }
        } catch (PermissionDeniedException e) {
            if (isCreate) {
                throw new SaveException(ExceptionUtils.getDebugException(e));
            } else {
                throw new UpdateException(ExceptionUtils.getDebugException(e));
            }
        }
        if (isCreate) {
            return scheduledTaskManager.save(scheduledTask);
        } else {
            scheduledTaskManager.update(scheduledTask);
            return scheduledTask.getGoid();
        }
    }

    private void validatePermitted(@NotNull final EntityType entityType,
                                   @NotNull final OperationType operationType) throws PermissionDeniedException {
        final User user = JaasUtils.getCurrentUser();

        try {
            if (!rbacServices.isPermittedForAnyEntityOfType(user, operationType, entityType)) {
                throw new PermissionDeniedException(operationType, entityType);
            }
        } catch (FindException e) {
            throw new PermissionDeniedException(operationType, entityType, e.getMessage());
        }
    }

    @Override
    public void deleteScheduledTask(ScheduledTask scheduledTask) throws DeleteException {
        scheduledTaskManager.delete(scheduledTask);
    }
}
