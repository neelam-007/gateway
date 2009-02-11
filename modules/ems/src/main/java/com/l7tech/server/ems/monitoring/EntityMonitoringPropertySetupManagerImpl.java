package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.*;

/**
 * The implementation of EntityMonitoringPropertySetupManager.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Feb 6, 2009
 * @since Enterprise Manager 1.0
 */
public class EntityMonitoringPropertySetupManagerImpl extends HibernateEntityManager<EntityMonitoringPropertySetup, EntityHeader> implements EntityMonitoringPropertySetupManager {
    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return EntityMonitoringPropertySetup.class;
    }

    @Override
    public String getTableName() {
        return "entity_monitoring_property_setup";
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return EntityMonitoringPropertySetup.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    /**
     * Gets the property setup uniquess constraints to check if there exists a duplicate setup.
     * Every pair of entity guid and property type is unique.
     */
    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(EntityMonitoringPropertySetup propertySetup) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("entityGuid", propertySetup.getEntityGuid());
        attrs.put("propertyType", propertySetup.getPropertyType());
        return Arrays.asList(attrs);
    }

    @Override
    public List<EntityMonitoringPropertySetup> findByEntityGuid(final String entityGuid) throws FindException {
        if (entityGuid == null) {
            return null;
        }

        try {
            //noinspection unchecked
            return (List<EntityMonitoringPropertySetup>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("entityGuid", entityGuid));
                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find the property setup for the entity (GUID = '" + entityGuid + "').", e);
        }
    }

    @Override
    public EntityMonitoringPropertySetup findByEntityGuidAndPropertyType(final String entityGuid, final String propertyType) throws FindException {
        if (entityGuid == null || propertyType == null) {
            return null;
        }

        try {
            return (EntityMonitoringPropertySetup)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("entityGuid", entityGuid));
                    crit.add(Restrictions.eq("propertyType", propertyType));
                    return crit.uniqueResult();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Cannot find the property setup for the entity (GUID = '" + entityGuid + "').", e);
        }
    }
}
