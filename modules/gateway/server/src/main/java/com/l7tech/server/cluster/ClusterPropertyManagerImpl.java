/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.admin.AuditSigningStatusChange;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
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
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class ClusterPropertyManagerImpl
        extends HibernateEntityManager<ClusterProperty, EntityHeader>
        implements ClusterPropertyManager {
    private final Logger logger = Logger.getLogger(ClusterPropertyManagerImpl.class.getName());

    private final String HQL_FIND_BY_NAME =
            "from " + getTableName() +
                    " in class " + ClusterProperty.class.getName() +
                    " where " + getTableName() + ".name = ?";

    public ClusterPropertyManagerImpl() {}

    /**
     * @return may return null if the property is not set. will return the property value otherwise
     */
    @Override
    @Transactional(readOnly=true)
    public String getProperty(String key) throws FindException {
        ClusterProperty prop = findByKey(key);
        if (prop != null) {
            return prop.getValue();
        }
        return null;
    }

    private void propertyChangeMonitor(ClusterProperty p) {
        if (p.getName().equals( ServerConfigParams.PARAM_CONFIG_AUDIT_SIGN_CLUSTER )) {
            if (p.getValue().equals("true")) {
                applicationContext.publishEvent(new AuditSigningStatusChange(this, "on"));
            } else {
                applicationContext.publishEvent(new AuditSigningStatusChange(this, "off"));
            }
        }
    }

    @Override
    public ClusterProperty putProperty(String key, String value) throws FindException, SaveException, UpdateException {
        ClusterProperty prop = findByKey(key);
        if (prop == null) {
            prop = new ClusterProperty(key, value);
            Goid goid = save(prop);
            if (!goid.equals(prop.getGoid())) prop.setGoid(goid);
            return prop;
        }

        prop.setValue(value);
        update(prop);
        return prop;
    }

    @Override
    public Goid save(ClusterProperty p) throws SaveException {
        // monitor certain property changes
        propertyChangeMonitor(p);
        return super.save(p);
    }

    @Override
    public void update(ClusterProperty p) throws UpdateException {
        // monitor certain property changes
        propertyChangeMonitor(p);
        super.update(p);
    }

    private ClusterProperty describe(ClusterProperty cp) {
        if (cp == null) return null;
        ServerConfig sc = ServerConfig.getInstance();
        String serverPropName = sc.getNameFromClusterName(cp.getName());
        //always use the description from the serverconfig.properties file if one exists. If not then the description in the database will be used.
        if (serverPropName != null) cp.setProperty(ClusterProperty.DESCRIPTION_PROPERTY_KEY, sc.getPropertyDescription(serverPropName));
        return cp;
    }

    @Override
    public ClusterProperty getCachedEntityByName(String name, int maxAge) throws FindException {
        ClusterProperty cp = super.getCachedEntityByName(name, maxAge);
        return cp == null ? null : describe(cp);
    }

    @Override
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
                @Override
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
        Goid goid = null;
        try {
            goid = Goid.parseGoid(header.getStrId());
        } catch (IllegalArgumentException e) {
            // do nothing
        }
        if(goid != null) {
            cp = super.findByPrimaryKey(EntityTypeRegistry.getEntityClass(EntityType.CLUSTER_PROPERTY), goid);
        }

        // fallback to name lookup
        return cp != null ? cp : super.findByUniqueName(header.getName());
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return ClusterProperty.class;
    }
}
