package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateGoidEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Feb 3, 2009
 * @since Enterprise Manager 1.0
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SystemMonitoringNotificationRulesManagerImpl extends HibernateGoidEntityManager<SystemMonitoringNotificationRule, EntityHeader> implements SystemMonitoringNotificationRulesManager {

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return SystemMonitoringNotificationRule.class;
    }

    @Override
    public String getTableName() {
        return "system_monitoring_notification_rule";
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return SystemMonitoringNotificationRule.class;
    }

    @Override
    public SystemMonitoringNotificationRule findByGuid(final String guid) throws FindException {
       try {
            return (SystemMonitoringNotificationRule)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria crit = session.createCriteria(getImpClass());
                    if (guid == null) {
                        crit.add(Restrictions.isNull("guid"));
                    } else {
                        crit.add(Restrictions.eq("guid", guid));
                    }
                    return crit.uniqueResult();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find System Monitoring Notification Rule by GUID: " + guid, e);
        }
    }

    @Override
    public void deleteByGuid(String guid) throws FindException, DeleteException {
        SystemMonitoringNotificationRule rule = findByGuid(guid);
        delete(rule);
    }
}
