package com.l7tech.cluster;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.util.Collection;
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

    /**
     * retrieves all service usage recorded in database
     * @return a collection filled with ServiceUsage objects
     */
    public Collection getAll() throws FindException {
        try {
            String queryall = "from " + TABLE_NAME + " in class " + ServiceUsage.class.getName();
            Session session = getSession();
            return session.find(queryall);
        } catch (HibernateException e) {
            String msg = "could not retreive service usage obj";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
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

            String delQuery = "from " + TABLE_NAME + " in class " + ServiceUsage.class.getName() +
                              " where " + TABLE_NAME + "." + NODE_ID_COLUMN_NAME +
                              " = \'" + nodeid + "\'";
            session.delete(delQuery);
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, "error clearing table", e);
        }
    }

    private boolean isAlreadyInDB(ServiceUsage arg) {
        String query = "from " + TABLE_NAME + " in class " + ServiceUsage.class.getName() +
                          " where " + TABLE_NAME + "." + NODE_ID_COLUMN_NAME +
                          " = \'" + arg.getNodeid() + "\'" + " and " + TABLE_NAME + "." + SERVICE_ID_COLUMN_NAME +
                          " = \'" + arg.getServiceid() + "\'";
        try {
            Session session = getSession();
            List res = session.find(query);
            if (res == null || res.isEmpty()) return false;
            return true;
        } catch (HibernateException e) {
            String msg = "could not retreive service usage obj";
            logger.log(Level.SEVERE, msg, e);
            return false;
        }
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    public static final String TABLE_NAME = "service_usage";
    public static final String NODE_ID_COLUMN_NAME = "nodeid";
    public static final String SERVICE_ID_COLUMN_NAME = "serviceid";
}
