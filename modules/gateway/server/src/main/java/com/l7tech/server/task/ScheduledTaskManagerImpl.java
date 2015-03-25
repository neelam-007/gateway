package com.l7tech.server.task;

import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.HibernateEntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

/**
 */
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
public class ScheduledTaskManagerImpl
        extends HibernateEntityManager<ScheduledTask, EntityHeader> implements ScheduledTaskManager {

    private static final Logger logger = Logger.getLogger(ScheduledTaskManagerImpl.class.getName());

    @Override
    public Class<? extends Entity> getImpClass() {
        return ScheduledTask.class;
    }


}
