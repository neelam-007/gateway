/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.cluster;

import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.admin.AuditSigningStatusChange;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hibernate manager for read/write access to the cluster_properties table.
 *
 * @author flascelles@layer7-tech.com
 */
@Transactional(propagation=Propagation.REQUIRED)
public class ClusterPropertyManagerImpl
        extends HibernateEntityManager<ClusterProperty, EntityHeader>
        implements ClusterPropertyManager, ApplicationContextAware {
    private final Logger logger = Logger.getLogger(ClusterPropertyManagerImpl.class.getName());
    private ApplicationContext applicationContext;

    private final String HQL_FIND_BY_NAME =
            "from " + getTableName() +
                    " in class " + ClusterProperty.class.getName() +
                    " where " + getTableName() + ".name = ?";

    public ClusterPropertyManagerImpl() {}

    /**
     * @return may return null if the property is not set. will return the property value otherwise
     */
    @Transactional(readOnly=true)
    public String getProperty(String key) throws FindException {
        ClusterProperty prop = findByKey(key);
        if (prop != null) {
            return prop.getValue();
        }
        return null;
    }

    private void propertyChangeMonitor(ClusterProperty p) {
        if (p.getName().equals(ServerConfig.CONFIG_AUDIT_SIGN_CLUSTER)) {
            if (p.getValue().equals("true")) {
                applicationContext.publishEvent(new AuditSigningStatusChange(this, "on"));
            } else {
                applicationContext.publishEvent(new AuditSigningStatusChange(this, "off"));
            }
        }
    }

    public long save(ClusterProperty p) throws SaveException {
        // monitor certain property changes
        propertyChangeMonitor(p);
        return super.save(p);
    }

    public void update(ClusterProperty p) throws UpdateException {
        // monitor certain property changes
        propertyChangeMonitor(p);
        super.update(p);
    }

    private ClusterProperty describe(ClusterProperty cp) {
        if (cp == null) return null;
        ServerConfig sc = ServerConfig.getInstance();
        String serverPropName = sc.getNameFromClusterName(cp.getName());
        if (serverPropName != null) cp.setDescription(sc.getPropertyDescription(serverPropName));
        return cp;
    }

    public ClusterProperty getCachedEntityByName(String name, int maxAge) throws FindException {
        ClusterProperty cp = super.getCachedEntityByName(name, maxAge);
        return cp == null ? null : describe(cp);
    }

    public Collection<ClusterProperty> findAll() throws FindException {
        Collection<ClusterProperty> all = super.findAll();
        for (ClusterProperty clusterProperty : all) {
            describe(clusterProperty);
        }
        return all;
    }

    @Transactional(readOnly=true)
    private ClusterProperty findByKey(final String key) throws FindException {
        try {
            return (ClusterProperty)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    // Prevent reentrant ClusterProperty lookups from flushing in-progress writes
                    Query q = session.createQuery(HQL_FIND_BY_NAME);
                    q.setString(0, key);
                    return describe((ClusterProperty)q.uniqueResult());
                }
            });
        } catch (HibernateException e) {
            String msg = "error retrieving property";
            logger.log(Level.WARNING, msg, e);
            throw new FindException(msg, e);
        }
    }

/*
    public void update(ClusterProperty clusterProperty) throws UpdateException {
        ClusterProperty old;
        try {
            old = findByUniqueName(clusterProperty.getName());
        } catch (FindException e) {
            throw new UpdateException("Couldn't find original version", e);
        }

        try {
            if (old == null) {
                getHibernateTemplate().save(clusterProperty);
            } else {
                old.setName(clusterProperty.getName());
                old.setValue(clusterProperty.getValue());
                getHibernateTemplate().merge(clusterProperty);
            }
        } catch (Exception e) {
            throw new UpdateException("Couldn't save new property", e);
        }
    }
*/

    public Class getImpClass() {
        return ClusterProperty.class;
    }

    public Class getInterfaceClass() {
        return ClusterProperty.class;
    }

    public String getTableName() {
        return "cluster_properties";
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
