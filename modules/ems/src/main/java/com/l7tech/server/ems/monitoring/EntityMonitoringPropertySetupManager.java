package com.l7tech.server.ems.monitoring;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

import java.util.List;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Feb 6, 2009
 * @since Enterprise Manager 1.0
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public interface EntityMonitoringPropertySetupManager extends EntityManager<EntityMonitoringPropertySetup, EntityHeader> {
    /**
     * Find all property setups for the given entity (Cluster or Node) guid.
     * @param entityGuid: the GUID of an entity such as SSG Cluster or SSG Node.
     * @return an EntityMonitoringPropertySetup object.
     * @throws FindException if there is a data access error.
     */
    public List<EntityMonitoringPropertySetup> findByEntityGuid(final String entityGuid) throws FindException;

    /**
     * Find the property setup by the given entity guid and property type.
     * @param entityGuid: the GUID of an entity such as SSG Cluster or SSG Node.
     * @param propertyType: the property type such as "auditSize", "cpuTemp", etc.
     * @return an EntityMonitoringPropertySetup object.
     * @throws FindException if there is a data access error.
     */
    EntityMonitoringPropertySetup findByEntityGuidAndPropertyType(String entityGuid, String propertyType) throws FindException;
}
