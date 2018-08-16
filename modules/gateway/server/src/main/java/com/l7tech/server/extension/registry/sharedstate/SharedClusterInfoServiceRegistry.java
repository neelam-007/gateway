package com.l7tech.server.extension.registry.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterInfoService;
import com.l7tech.server.cluster.LocalClusterInfoService;
import com.l7tech.server.extension.registry.AbstractRegistryImpl;

import java.util.logging.Logger;

public class SharedClusterInfoServiceRegistry extends AbstractRegistryImpl<ClusterInfoService> {
    private static final Logger LOGGER = Logger.getLogger(SharedClusterInfoServiceRegistry.class.getName());
    public static final String SYSPROP_CLUSTER_INFO_PROVIDER = "com.l7tech.server.extension.sharedClusterInfoProvider";

    public SharedClusterInfoServiceRegistry() {
        super();
        this.register(LocalClusterInfoService.KEY, new LocalClusterInfoService(), LocalClusterInfoService.KEY);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public ClusterInfoService getExtension(String key) {
        if (key == null || key.isEmpty()) {
            return super.getExtension(LocalClusterInfoService.KEY);
        }
        return super.getExtension(key);
    }
}
