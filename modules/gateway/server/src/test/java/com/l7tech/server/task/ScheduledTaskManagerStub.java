package com.l7tech.server.task;

import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerStub;

/**
 */
public class ScheduledTaskManagerStub extends EntityManagerStub<ScheduledTask, EntityHeader> implements ScheduledTaskManager {

    @Override
    public ScheduledTask findByPrimaryKey(Goid goid, boolean lock) throws FindException {
        return findByPrimaryKey(goid);
    }
}
