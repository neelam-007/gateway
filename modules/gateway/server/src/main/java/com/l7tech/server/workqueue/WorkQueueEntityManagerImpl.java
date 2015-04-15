package com.l7tech.server.workqueue;

import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.HibernateEntityManager;

public class WorkQueueEntityManagerImpl extends HibernateEntityManager<WorkQueue, EntityHeader> implements WorkQueueEntityManager{

    @Override
    public Class<? extends PersistentEntity> getImpClass() {
        return WorkQueue.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NAME;
    }

    @Override
    public WorkQueue getWorkQueueEntity(Goid id) throws FindException {
        return findByPrimaryKey(id);
    }

}
