package com.l7tech.server.ems.enterprise;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The implementation for SsgNodeManager that manages the ssg_node table.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 14, 2008
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SsgNodeManagerImpl extends HibernateGoidEntityManager<SsgNode, EntityHeader> implements SsgNodeManager {

    //- PUBLIC

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
    public SsgNode findByGuid( final String guid ) throws FindException {
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
            throw new FindException("Cannot find Gateway Node by GUID: " + guid, e);
        }
    }

    //- PROTECTED
    
    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final SsgNode ssgNode ) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("ssgCluster", ssgNode.getSsgCluster());
        attrs.put("name", ssgNode.getName());
        return Arrays.asList(attrs);
    }

}
