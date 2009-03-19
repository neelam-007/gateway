/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.server.cluster;

import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.event.admin.AuditSigningStatusChange;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
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

    public ClusterProperty putProperty(String key, String value) throws FindException, SaveException, UpdateException {
        ClusterProperty prop = findByKey(key);
        if (prop == null) {
            prop = new ClusterProperty(key, value);
            long oid = save(prop);
            if (oid != prop.getOid()) prop.setOid(oid);
            return prop;
        }

        prop.setValue(value);
        update(prop);
        return prop;
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

    @Override
    public ClusterProperty findByHeader(EntityHeader header) throws FindException {
        if ( EntityType.CLUSTER_PROPERTY != header.getType() ) {
            throw new IllegalArgumentException("Invalid header type: " + header);
        }

        ClusterProperty cp = null;
        try {
            cp = super.findByPrimaryKey(EntityTypeRegistry.getEntityClass(EntityType.CLUSTER_PROPERTY), Long.parseLong(header.getStrId()));
        } catch (NumberFormatException e) {
            // do nothing
        }

        // fallback to name lookup
        return cp != null ? cp : super.findByUniqueName(header.getName());
    }

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
