package com.l7tech.server.workqueue;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.gateway.common.workqueue.WorkQueueManagerAdmin;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.util.JaasUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkQueueManagerAdminImpl extends AsyncAdminMethodsImpl implements WorkQueueManagerAdmin {

    private final WorkQueueEntityManager workQueueEntityManager;
    private final WorkQueueExecutorManager workQueueExecutorManager;
    private final RbacServices rbacServices;

    public WorkQueueManagerAdminImpl(WorkQueueEntityManager workQueueEntityManager,
                                     RbacServices rbacServices,
                                     WorkQueueExecutorManager workQueueExecutorManager) {
        this.workQueueEntityManager = workQueueEntityManager;
        this.rbacServices = rbacServices;
        this.workQueueExecutorManager = workQueueExecutorManager;
    }

    @Override
    public WorkQueue getWorkQueue(String name) throws FindException {
        return workQueueEntityManager.findByUniqueName(name);
    }

    @Override
    public List<WorkQueue> getAllWorkQueues() throws FindException {
        List<WorkQueue> workQueues = new ArrayList<>();
        workQueues.addAll(workQueueEntityManager.findAll());
        Collections.sort(workQueues);
        return workQueues;
    }

    @Override
    public List<String> getAllWorkQueueNames() throws FindException {
        User user = getCurrentUser();
        List<String> names = new ArrayList<>();
        for (WorkQueue entity : getAllWorkQueues()) {
            if (rbacServices.isPermittedForEntity(user, entity, OperationType.READ, null)) {
                names.add(entity.getName());
            }
        }
        return names;
    }

    protected User getCurrentUser() {
        return JaasUtils.getCurrentUser();
    }

    @Override
    public Goid saveWorkQueue(WorkQueue newEntity) throws UpdateException, SaveException, FindException {
        Goid goid;
        if (newEntity.getGoid().equals(Goid.DEFAULT_GOID)) {
            goid = workQueueEntityManager.save(newEntity);
        } else {
            // Edit
            final WorkQueue oldEntity = workQueueEntityManager.findByPrimaryKey(newEntity.getGoid());
            if (oldEntity == null) {
                throw new UpdateException("Cannot find existing work queue entity with GOID: " + newEntity.getId());
            }
            workQueueExecutorManager.updateWorkQueueExecutor(newEntity, oldEntity);
            workQueueEntityManager.update(newEntity);
            goid = newEntity.getGoid();
        }
        return goid;
    }

    @Override
    public void deleteWorkQueue(WorkQueue workQueue) throws DeleteException {
        workQueueExecutorManager.removeWorkQueueExecutor(workQueue);
        workQueueEntityManager.delete(workQueue);
    }
}
