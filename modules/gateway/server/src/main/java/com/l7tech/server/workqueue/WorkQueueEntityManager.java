package com.l7tech.server.workqueue;

import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

public interface WorkQueueEntityManager  extends EntityManager<WorkQueue, EntityHeader> {
    /**
     * retrieves the work queue entity from the dataStore by work queue name
     * @param workQueueName  name of the work queue
     * @return WorkQueueEntity
     * @throws FindException
     */
    WorkQueue getWorkQueueEntity(String workQueueName) throws FindException;
}
