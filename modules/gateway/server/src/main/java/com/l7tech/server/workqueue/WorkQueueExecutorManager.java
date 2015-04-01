package com.l7tech.server.workqueue;

import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;

import java.util.concurrent.ThreadPoolExecutor;

public interface WorkQueueExecutorManager {

    ThreadPoolExecutor getWorkQueueExecutor(String name);

    void removeWorkQueueExecutor(WorkQueue workQueueEntity);

    void removeWorkQueueExecutor(Goid goid);

    void updateWorkQueueExecutor(WorkQueue newEntity, WorkQueue oldEntity) throws UpdateException;
}