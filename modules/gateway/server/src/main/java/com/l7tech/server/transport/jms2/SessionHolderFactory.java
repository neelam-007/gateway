package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.server.transport.jms.JmsBag;


public class SessionHolderFactory {

    private SessionHolderFactory () {}

    public static SessionHolder createSessionHolder(final JmsEndpointConfig cfg,
                                                    final JmsBag bag,
                                                    final JmsResourceManagerConfig cacheConfig) {
        int poolSize = Integer.parseInt(cfg.getConnection().properties().getProperty(JmsConnection.PROP_SESSION_POOL_SIZE,
                String.valueOf(cacheConfig.getSessionPoolSize())));

        if (poolSize > 0) {
            return new PooledSessionHolder(cfg, bag, cacheConfig);
        } else {
            return new NonCachedSessionHolder(cfg, bag);
        }
    }

    public static SessionHolder createSessionHolder(final JmsEndpointConfig endpoint, final JmsBag newBag) throws Exception{
        return new SingleSessionHolder(endpoint, newBag);
    }
}
