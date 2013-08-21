package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

/**
 * The implementation of SsgClusterNotificationSetupManager.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Feb 7, 2009
 * @since Enterprise Manager 1.0
 */
public class SsgClusterNotificationSetupManagerImpl extends HibernateEntityManager<SsgClusterNotificationSetup, EntityHeader> implements SsgClusterNotificationSetupManager {
    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return SsgClusterNotificationSetup.class;
    }

    @Override
    public String getTableName() {
        return "ssgcluster_notification_setup";
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return SsgClusterNotificationSetup.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    @Override
    public SsgClusterNotificationSetup findByEntityGuid(final String ssgClusterGuid) throws FindException {
        try {
            return (SsgClusterNotificationSetup)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria crit = session.createCriteria(getImpClass());
                    if (ssgClusterGuid == null) {
                        crit.add(Restrictions.isNull("ssgClusterGuid"));
                    } else {
                        crit.add(Restrictions.eq("ssgClusterGuid", ssgClusterGuid));
                    }
                    return crit.uniqueResult();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find Gateway Cluster Notification Setup by Gateway Cluster GUID: " + ssgClusterGuid, e);
        }
    }

    @Override
    public void deleteBySsgClusterGuid(String guid) throws FindException, DeleteException {
        SsgClusterNotificationSetup setup = findByEntityGuid(guid);
        if (setup != null) {
            delete(findByEntityGuid(guid));
        }
    }
}
