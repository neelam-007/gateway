package com.l7tech.server.task;

import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;

/**
 */
public interface ScheduledTaskManager extends EntityManager<ScheduledTask, EntityHeader> {

    /**
     * Finds a the scheduled task object based on the primary key.
     *
     * @param goid          The primary key
     * @param lock          True to lock the object for update
     * @return              The ScheduledTask object if found.
     * @throws FindException
     */
    ScheduledTask findByPrimaryKey(Goid goid, boolean lock) throws FindException;
}
