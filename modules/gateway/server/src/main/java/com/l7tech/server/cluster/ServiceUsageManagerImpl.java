package com.l7tech.server.cluster;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.cluster.ServiceUsage;
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
 *
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class ServiceUsageManagerImpl extends HibernateDaoSupport implements ServiceUsageManager {
    private final String HQL_FIND_BY_NODE =
            "from " + TABLE_NAME +
                    " in class " + ServiceUsage.class.getName() +
                    " where " + TABLE_NAME + "." + NODE_ID_COLUMN_NAME + " = ?";

    private final String HQL_DELETE_BY_NODE =
            "delete from " + ServiceUsage.class.getName() + " as " + TABLE_NAME +
                    " where " + TABLE_NAME + "." + NODE_ID_COLUMN_NAME + " = :nodeid";

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
    @SuppressWarnings({ "unchecked" })
    @Override
    @Transactional(readOnly=true)
    public Collection<ServiceUsage> getAll() throws FindException {
        Session session = null;
        FlushMode old = null;
        try {
            session = getSession();
            old = session.getFlushMode();
            session.setFlushMode(FlushMode.MANUAL);
            List list = session.createQuery(HQL_FIND_ALL).list();
            Set<ServiceUsage> results = new HashSet<ServiceUsage>();
            results.addAll(list);
            return results;
        } catch (HibernateException e) {
            String msg = "could not retrieve service usage obj";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
        } finally {
            if (session != null && old != null) session.setFlushMode(old);
            releaseSession(session);
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    @Transactional(readOnly=true)
    public ServiceUsage[] findByNode(String nodeId) throws FindException {
        Session s = null;
        FlushMode old = null;
        try {
            s = getSession();
            old = s.getFlushMode();
            s.setFlushMode(FlushMode.MANUAL);
            Query q = s.createQuery(HQL_FIND_BY_NODE);
            q.setString(0, nodeId);
            List results = q.list();
            return (ServiceUsage[])results.toArray(new ServiceUsage[results.size()]);
        } catch (HibernateException e) {
            throw new FindException("Couldn't retrieve ServiceUsage", e);
        } finally {
            if (s != null && old != null) s.setFlushMode(old);
            releaseSession(s);
        }
    }

    /**
     * Finds the ServiceUsage records for the given {@link com.l7tech.gateway.common.service.PublishedService} OID for all cluster nodes
     * @throws FindException
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    @Transactional(readOnly=true)
    public ServiceUsage[] findByServiceGoid(Goid serviceGoid) throws FindException {
        Session s = null;
        FlushMode old = null;
        try {
            s = getSession();
            old = s.getFlushMode();
            s.setFlushMode(FlushMode.MANUAL);
            Query q = s.createQuery(HQL_FIND_BY_SERVICE);
            q.setParameter(0, serviceGoid);
            List results = q.list();
            return (ServiceUsage[])results.toArray(new ServiceUsage[results.size()]);
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
    @Override
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
    @Override
    public void clear(final String nodeid) throws DeleteException {
        try {
            getHibernateTemplate().execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(final Session session) throws HibernateException, SQLException {
                    // Use a bulk delete to ensure that a replicable SQL statement is run
                    // even if there is nothing in the table (see bug 4615)
                    session.createQuery( HQL_DELETE_BY_NODE )
                            .setString("nodeid", nodeid)
                            .executeUpdate();
                    return null;
                }
            });
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, "error clearing table", e);
        }
    }

    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private final Logger logger = Logger.getLogger(getClass().getName());
}
