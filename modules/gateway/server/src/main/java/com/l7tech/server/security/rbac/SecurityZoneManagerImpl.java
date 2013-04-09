package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.HibernateEntityManager;

/**
 * Gateway server implementation of Hibernate entity manager for the entities representing Security Zones.
 */
public class SecurityZoneManagerImpl extends HibernateEntityManager<SecurityZone, EntityHeader> implements SecurityZoneManager {
    @Override
    public Class<? extends Entity> getImpClass() {
        return SecurityZone.class;
    }
}
