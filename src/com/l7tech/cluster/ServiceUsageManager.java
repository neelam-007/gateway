package com.l7tech.cluster;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
public class ServiceUsageManager extends HibernateDaoSupport {
    private final String HQL_DELETE_BY_MAC =
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
    public Collection getAll() throws FindException {
        try {
            Session session = getSession();
            return session.createQuery(HQL_FIND_ALL).list();
        } catch (HibernateException e) {
            String msg = "could not retreive service usage obj";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
        }
    }

    /**
     * Finds the ServiceUsage records for the given {@link com.l7tech.service.PublishedService} OID for all cluster nodes
     * @return
     * @throws FindException
     */
    public ServiceUsage[] findByServiceOid(long serviceOid) throws FindException {
        try {
            Query q = getSession().createQuery(HQL_FIND_BY_SERVICE);
            q.setLong(0, serviceOid);
            List results = q.list();
            return (ServiceUsage[])results.toArray(new ServiceUsage[0]);
        } catch (HibernateException e) {
            throw new FindException("Couldn't retrieve ServiceUsage", e);
        }
    }

    /**
     * updates service_usage table with new information
     */
    public void record(ServiceUsage data) throws UpdateException {
        try {
            Session session = getSession();
            session.save(data);
            //session.saveOrUpdate(data);
            /*if (isAlreadyInDB(data)) {
                session.update(data);
            } else {
                session.save(data);
            }*/
        } catch (HibernateException e) {
            String msg = "could not record this service usage obj";
            logger.log(Level.SEVERE, msg, e);
            throw new UpdateException(msg, e);
        }
    }

    /**
     * clears the table of existing entries for this server
     */
    public void clear(String nodeid) throws DeleteException {
        try {
            Session session = getSession();
            Query q = session.createQuery(HQL_DELETE_BY_MAC);
            q.setString(0, nodeid);
            for (Iterator i = q.iterate(); i.hasNext();) {
                session.delete(i.next());
            }
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, "error clearing table", e);
        }
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    public static final String TABLE_NAME = "service_usage";
    public static final String NODE_ID_COLUMN_NAME = "nodeid";
    public static final String SERVICE_ID_COLUMN_NAME = "serviceid";
}
