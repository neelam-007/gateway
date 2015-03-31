package com.l7tech.server.task;

import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
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


    @Override
    public ScheduledTask findByPrimaryKey(final Goid goid, final boolean lock) throws FindException {
         try {
            return (ScheduledTask) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria criteria = session.createCriteria(ScheduledTask.class);
                    criteria.add(Restrictions.eq("goid", goid));
                    if ( lock ) {
                        criteria.setLockMode(LockMode.UPGRADE);
                    }
                    return criteria.uniqueResult();
                }
            });
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, org.hibernate.ObjectNotFoundException.class) ||
                    ExceptionUtils.causedBy(e, ObjectDeletedException.class)) {
                return null;
            }
            throw new FindException("Data access error ", e);
        }
    }
}
