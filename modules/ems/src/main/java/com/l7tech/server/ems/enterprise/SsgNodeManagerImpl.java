package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.sql.SQLException;

/**
 * The implementation for SsgNodeManager that manages the ssg_node table.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 14, 2008
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SsgNodeManagerImpl extends HibernateEntityManager<SsgNode, EntityHeader> implements SsgNodeManager {

    @Override
    public Class<SsgNode> getImpClass() {
        return SsgNode.class;
    }

    @Override
    public Class<SsgNode> getInterfaceClass() {
        return SsgNode.class;
    }

    @Override
    public String getTableName() {
        return "ssg_node";
    }

    @Override
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
