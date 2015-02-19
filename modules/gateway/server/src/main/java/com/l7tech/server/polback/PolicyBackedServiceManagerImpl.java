package com.l7tech.server.polback;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.server.HibernateEntityManager;

/**
 * Implementation of entity manager for policy backed service.
 */
public class  PolicyBackedServiceManagerImpl extends HibernateEntityManager<PolicyBackedService,EntityHeader> implements PolicyBackedServiceManager {
    @Override
    public Class<? extends Entity> getImpClass() {
        return PolicyBackedService.class;
    }
}
