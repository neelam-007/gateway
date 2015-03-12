package com.l7tech.server.task;

import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.gateway.common.task.ScheduledTaskAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.server.policy.CustomKeyValueStoreManager;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

/**
 * Created by luiwy01 on 3/9/2015.
 */
public class ScheduledTaskAdminImpl implements ScheduledTaskAdmin {

    @Inject
    private ScheduledTaskManager scheduledTaskManager;

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
        return scheduledTaskManager.save(scheduledTask);
    }

    @Override
    public void deleteScheduledTask(ScheduledTask scheduledTask) throws DeleteException {
        scheduledTaskManager.delete(scheduledTask);
    }
}
