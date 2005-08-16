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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.orm.hibernate.support.HibernateDaoSupport;
import net.sf.hibernate.HibernateException;

/**
 * Hibernate manager for read/write access to the cluster_properties table.
 *
 * todo, register the manager (spring), plug into admin layer
 * @author flascelles@layer7-tech.com
 */
public class ClusterPropertyManager extends HibernateDaoSupport {
    private static final String TABLE_NAME = "cluster_properties";
    private final Logger logger = Logger.getLogger(ClusterPropertyManager.class.getName());

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
        String query = "from " + TABLE_NAME + " in class " + ClusterProperty.class.getName() +
                       " where " + TABLE_NAME + ".propkey" + " = \'" + key + "\'";
        List hibResults = null;
        try {
            hibResults = getSession().find(query);
        }  catch (HibernateException e) {
            String msg = "error retrieving property";
            logger.log(Level.WARNING, msg, e);
        }
        if (hibResults == null || hibResults.isEmpty()) {
            return null;
        }
        switch (hibResults.size()) {
            case 1: {
                ClusterProperty prop = (ClusterProperty)hibResults.get(0);
                return prop.getValue();
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
        String existingVal = null;
        try {
            existingVal = getProperty(key);
        } catch (FindException e) {
            logger.log(Level.WARNING, "error getting existing value", e);
        }
        boolean alreadyExists = (existingVal != null);
        ClusterProperty row = new ClusterProperty();
        row.setKey(key);
        row.setValue(value);
        if (value == null) {
            // this is meant to be a deletion
            row.setValue(existingVal);
            try {
                getSession().delete(row);
            } catch (HibernateException e) {
                String msg = "exception deleting property for key = " + key;
                logger.log(Level.WARNING, msg, e);
                throw new DeleteException(msg, e);
            }
        } else if (alreadyExists) {
            try {
                getSession().update(row);
            } catch (HibernateException e) {
                String msg = "exception updating property for key = " + key;
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            }
        } else {
            try {
                getSession().save(row);
            } catch (HibernateException e) {
                String msg = "exception saving property for key = " + key;
                logger.log(Level.WARNING, msg, e);
                throw new SaveException(msg, e);
            }
        }
    }
}
