package com.l7tech.server.workqueue;

import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;

public class WorkQueueEntityManagerStub extends EntityManagerStub<WorkQueue, EntityHeader>
        implements WorkQueueEntityManager {

    @Override
    public WorkQueue getWorkQueueEntity(String workQueueName) throws FindException {
        return null;
    }


}
