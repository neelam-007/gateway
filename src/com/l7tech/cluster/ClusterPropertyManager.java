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

import java.util.List;

import org.springframework.orm.hibernate.support.HibernateDaoSupport;

/**
 * Hibernate manager for read/write access to the cluster_properties table.
 *
 * todo, register the manager (spring), create table, implement
 * @author flascelles@layer7-tech.com
 */
public class ClusterPropertyManager extends HibernateDaoSupport {

    public ClusterPropertyManager() {}

    /**
     * @return a list containing ClusterProperty objects (never null)
     */
    public List getAllProperties() throws FindException {
        // todo
        return null;
    }

    /**
     * @return may return null if the property is not set. will return the property value otherwise
     */
    public String getProperty(String key) throws FindException {
        // todo
        return null;
    }

    /**
     * set new value for the property. value set to null will delete the property from the table
     */
    public void setProperty(String key, String value) throws SaveException, UpdateException {
        // todo
    }
}
