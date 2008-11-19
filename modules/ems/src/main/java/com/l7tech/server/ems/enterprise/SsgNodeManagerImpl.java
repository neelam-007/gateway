package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

/**
 * The implementation for SsgNodeManager that manages the ssg_node table.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 14, 2008
 */
public class SsgNodeManagerImpl extends HibernateEntityManager<SsgNode, EntityHeader> implements SsgNodeManager {

    public Class<? extends Entity> getImpClass() {
        return SsgNode.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return SsgNode.class;
    }

    public String getTableName() {
        return "ssg_node";
    }

    public SsgNode findByGuid(final String guid) throws FindException {
        try {
            return (SsgNode)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
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
            throw new FindException("Cannot find SSG Node by GUID: " + guid, e);
        }
    }
}
