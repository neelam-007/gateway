package com.l7tech.server.service;

import com.l7tech.server.policy.PolicyCache;
import com.l7tech.cluster.ServiceUsageManager;
import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.service.PublishedService;
import com.l7tech.common.util.Decorator;
import com.l7tech.objectmodel.ObjectModelException;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;
import java.util.Timer;

/**
 * ServiceCache with mods for test context.
 */
public class ServiceCacheStub extends ServiceCache {

    /**
     *
     */
    public ServiceCacheStub(final PolicyCache policyCache,
                            final PlatformTransactionManager transactionManager,
                            final ServiceManager serviceManager,
                            final ServiceUsageManager serviceUsageManager,
                            final ClusterInfoManager clusterInfoManager,
                            final Collection<Decorator<PublishedService>> decorators,
                            final Timer timer) {
        super( policyCache,
               transactionManager,
               serviceManager,
               serviceUsageManager,
               clusterInfoManager,
               decorators,
               timer );
    }

    @Override
    public void initializeServiceCache() throws ObjectModelException {
        super.initializeServiceCache();
    }
}
