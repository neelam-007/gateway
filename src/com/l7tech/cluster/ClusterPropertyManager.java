/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.cluster;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.event.admin.ClusterPropertyEvent;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.orm.hibernate.support.HibernateDaoSupport;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;
import net.sf.hibernate.HibernateException;

/**
 * Hibernate manager for read/write access to the cluster_properties table.
 *
 * @author flascelles@layer7-tech.com
 */
public class ClusterPropertyManager extends HibernateDaoSupport implements ApplicationContextAware {
    private static final String TABLE_NAME = "cluster_properties";
    private final Logger logger = Logger.getLogger(ClusterPropertyManager.class.getName());
    private ApplicationContext applicationContext;

    public ClusterPropertyManager() {}

    /**
     * @return a list containing ClusterProperty objects (never null)
     */
    public List getAllProperties() throws FindException {
        String queryall = "from " + TABLE_NAME + " in class " + ClusterProperty.class.getName();
        try {
            return getSession().find(queryall);
        }  catch (HibernateException e) {
            String msg = "error retrieving cluster properties";
            logger.log(Level.WARNING, msg, e);
            throw new FindException(msg, e);
        }
    }

    /**
     * @return may return null if the property is not set. will return the property value otherwise
     */
    public String getProperty(String key) throws FindException {
        ClusterProperty prop = getRowObject(key);
        if (prop != null) {
            return prop.getValue();
        }
        return null;
    }

    private ClusterProperty getRowObject(String key) throws FindException {
        String query = "from " + TABLE_NAME + " in class " + ClusterProperty.class.getName() +
                       " where " + TABLE_NAME + ".key" + " = \'" + key + "\'";
        List hibResults = null;
        try {
            hibResults = getSession().find(query);
        }  catch (HibernateException e) {
            String msg = "error retrieving property";
            logger.log(Level.WARNING, msg, e);
            throw new FindException(msg, e);
        }
        if (hibResults == null || hibResults.isEmpty()) {
            logger.finest("property " + key + " does not exist");
            return null;
        }
        switch (hibResults.size()) {
            case 1: {
                ClusterProperty prop = (ClusterProperty)hibResults.get(0);
                return prop;
            }
            default:
                logger.warning("this should not happen. more than one entry found" +
                                          "for key: " + key);
                break;
        }
        return null;
    }

    /**
     * set new value for the property. value set to null will delete the property from the table
     */
    public void setProperty(String key, String value) throws SaveException, UpdateException, DeleteException {
        // try to get the prop
        ClusterProperty existingVal = null;
        try {
            existingVal = getRowObject(key);
        } catch (FindException e) {
            logger.log(Level.WARNING, "error getting existing value", e);
        }
        boolean alreadyExists = (existingVal != null);

        if (value == null) {
            // this is meant to be a deletion
            try {
                if (existingVal != null) {
                    getSession().delete(existingVal);
                    applicationContext.publishEvent(new ClusterPropertyEvent(existingVal, ClusterPropertyEvent.REMOVED));
                } else {
                    logger.info("null set on a property that already did not exist?");
                }
            } catch (HibernateException e) {
                String msg = "exception deleting property for key = " + key;
                logger.log(Level.WARNING, msg, e);
                throw new DeleteException(msg, e);
            }
        } else if (alreadyExists) {
            try {
                existingVal.setValue(value);
                getSession().update(existingVal);
                applicationContext.publishEvent(new ClusterPropertyEvent(existingVal, ClusterPropertyEvent.CHANGED));
            } catch (HibernateException e) {
                String msg = "exception updating property for key = " + key;
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            }
        } else {
            try {
                ClusterProperty row = new ClusterProperty();
                row.setKey(key);
                row.setValue(value);
                getSession().save(row);
                applicationContext.publishEvent(new ClusterPropertyEvent(row, ClusterPropertyEvent.ADDED));
            } catch (HibernateException e) {
                String msg = "exception saving property for key = " + key;
                logger.log(Level.WARNING, msg, e);
                throw new SaveException(msg, e);
            }
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
