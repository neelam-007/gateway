package com.l7tech.cluster;

import com.l7tech.objectmodel.*;
import com.l7tech.logging.LogManager;

import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.SQLException;

import net.sf.hibernate.Session;
import net.sf.hibernate.HibernateException;

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
public class ServiceUsageManager {

    /**
     * retrieves all service usage recorded in database
     * @return a collection filled with ServiceUsage objects
     */
    public Collection getAll() throws FindException {
        try {
            String queryall = "from " + TABLE_NAME + " in class " + ServiceUsage.class.getName();
            HibernatePersistenceContext pc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            Session session = pc.getSession();
            return session.find(queryall);
        } catch (SQLException e) {
            String msg = "could not retreive service usage obj";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
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
            HibernatePersistenceContext pc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            Session session = pc.getSession();
            session.save(data);
        } catch (SQLException e) {
            String msg = "could not record this service usage obj";
            logger.log(Level.SEVERE, msg, e);
            throw new UpdateException(msg, e);
        } catch (HibernateException e) {
            String msg = "could not record this service usage obj";
            logger.log(Level.SEVERE, msg, e);
            throw new UpdateException(msg, e);
        }
    }

    /**
     * clears the table
     */
    public void clear() throws DeleteException {
        try {
            HibernatePersistenceContext pc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            Session session = pc.getSession();
            session.delete("from " + TABLE_NAME + " in class " + ServiceUsage.class.getName());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error clearing table", e);
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, "error clearing table", e);
        }
    }

    private final Logger logger = LogManager.getInstance().getSystemLogger();
    public static final String TABLE_NAME = "service_usage";
}
