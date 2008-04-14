package com.l7tech.cluster;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hibernate abstraction of the service_usage table.
 *
 * This table is used to record usage statistics for a particular service on a particular node.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 22, 2003<br/>
 * $Id$<br/>
 *
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class ServiceUsageManagerImpl extends HibernateDaoSupport implements ServiceUsageManager {
    private final String HQL_FIND_BY_NODE =
            "from " + TABLE_NAME +
                    " in class " + ServiceUsage.class.getName() +
                    " where " + TABLE_NAME + "." + NODE_ID_COLUMN_NAME + " = ?";

    private final String HQL_FIND_ALL =
            "from " + TABLE_NAME + " in class " + ServiceUsage.class.getName();

    private final String HQL_FIND_BY_SERVICE =
            "FROM " + TABLE_NAME +
                " IN CLASS " + ServiceUsage.class.getName() +
                " WHERE " + SERVICE_ID_COLUMN_NAME + " = ?";

    /**
     * retrieves all service usage recorded in database
     * @return a collection filled with ServiceUsage objects
     */
    @Transactional(readOnly=true)
    public Collection<ServiceUsage> getAll() throws FindException {
        Session session = null;
        FlushMode old = null;
        try {
            session = getSession();
            old = session.getFlushMode();
            session.setFlushMode(FlushMode.NEVER);
            List list = session.createQuery(HQL_FIND_ALL).list();
            Set<ServiceUsage> results = new HashSet<ServiceUsage>();
            results.addAll(list);
            return results;
        } catch (HibernateException e) {
            String msg = "could not retreive service usage obj";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
        } finally {
            if (session != null && old != null) session.setFlushMode(old);
            releaseSession(session);
        }
    }

    @Transactional(readOnly=true)
    public ServiceUsage[] findByNode(String nodeId) throws FindException {
        Session s = null;
        FlushMode old = null;
        try {
            s = getSession();
            old = s.getFlushMode();
            s.setFlushMode(FlushMode.NEVER);
            Query q = s.createQuery(HQL_FIND_BY_NODE);
            q.setString(0, nodeId);
            List results = q.list();
            return (ServiceUsage[])results.toArray(new ServiceUsage[0]);
        } catch (HibernateException e) {
            throw new FindException("Couldn't retrieve ServiceUsage", e);
        } finally {
            if (s != null && old != null) s.setFlushMode(old);
            releaseSession(s);
        }
    }

    /**
     * Finds the ServiceUsage records for the given {@link com.l7tech.service.PublishedService} OID for all cluster nodes
     * @throws FindException
     */
    @Transactional(readOnly=true)
    public ServiceUsage[] findByServiceOid(long serviceOid) throws FindException {
        Session s = null;
        FlushMode old = null;
        try {
            s = getSession();
            old = s.getFlushMode();
            s.setFlushMode(FlushMode.NEVER);
            Query q = s.createQuery(HQL_FIND_BY_SERVICE);
            q.setLong(0, serviceOid);
            List results = q.list();
            return (ServiceUsage[])results.toArray(new ServiceUsage[0]);
        } catch (HibernateException e) {
            throw new FindException("Couldn't retrieve ServiceUsage", e);
        } finally {
            if (s != null && old != null) s.setFlushMode(old);
            releaseSession(s);
        }
    }

    /**
     * updates service_usage table with new information
     */
    public void record(ServiceUsage data) throws UpdateException {
        try {
            getHibernateTemplate().save(data);
        } catch (HibernateException e) {
            String msg = "could not record this service usage obj";
            logger.log(Level.SEVERE, msg, e);
            throw new UpdateException(msg, e);
        }
    }

    /**
     * clears the table of existing entries for this server
     */
    public void clear(final String nodeid) throws DeleteException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_BY_NODE);
                    q.setString(0, nodeid);
                    for (Iterator i = q.iterate(); i.hasNext();) {
                        session.delete(i.next());
                    }
                    return null;
                }
            });
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, "error clearing table", e);
        }
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
