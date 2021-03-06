package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.HibernateEntityManager;

/**
 * Manager implementation for service resolution configurations.
 */
public class ResolutionConfigurationManagerImpl extends HibernateEntityManager<ResolutionConfiguration, EntityHeader> implements ResolutionConfigurationManager {

    @Override
    public Class<ResolutionConfiguration> getImpClass() {
        return ResolutionConfiguration.class;
    }

}
