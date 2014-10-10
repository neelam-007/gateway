package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.l7tech.external.assertions.mqnative.server.MqNativeCachedConnectionPool.CachedConnection;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
import org.apache.commons.pool.PoolableObjectFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.closeQuietly;

/**
 * Factory to create poolable cached connections.
 */
class PoolableCachedConnectionFactory implements PoolableObjectFactory<CachedConnection> {

    private static final Logger logger = Logger.getLogger(PoolableCachedConnectionFactory.class.getName());
    private MqNativeEndpointConfig mqNativeEndpointConfig = null;
    // mqQueueManager is used for mocking unit tests
    private MQQueueManager mqQueueManager = null;

    PoolableCachedConnectionFactory(final MqNativeEndpointConfig cfg) {
        mqNativeEndpointConfig = cfg;
    }

    // This constructor is mainly for testing purposes
    PoolableCachedConnectionFactory(final MQQueueManager qm) {
        mqQueueManager = qm;
    }

    @Override
    public CachedConnection makeObject() throws Exception {
        // create the new manager for the endpoint
        CachedConnection newConnection = new CachedConnection(new MQQueueManager(mqNativeEndpointConfig.getQueueManagerName(),
                mqNativeEndpointConfig.getQueueManagerProperties()), mqNativeEndpointConfig.getMqEndpointKey().toString(),
                mqNativeEndpointConfig.getMqEndpointKey().getVersion());

        logger.log(Level.INFO, "New MQ QueueManager connection created ({0}), version {1}",
                new Object[]{newConnection.getName(), mqNativeEndpointConfig.getMqEndpointKey().getVersion()});

        return newConnection;
    }

    @Override
    public void destroyObject(CachedConnection cachedConnection) throws Exception {
        logger.log(Level.INFO, "Closing MQ queue manager ({0}), version {1}",
                new Object[]{cachedConnection.getName(), cachedConnection.getResourceVersion()});
        closeQuietly(cachedConnection.getQueueManager(), Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
            @Override
            public void call(final MQQueueManager mqQueueManager) throws MQException {
                mqQueueManager.disconnect();
            }
        }));
    }

    @Override
    public boolean validateObject(CachedConnection obj) {
        return true;
    }

    @Override
    public void activateObject(CachedConnection obj) throws Exception {
        // do nothing
    }

    @Override
    public void passivateObject(CachedConnection obj) throws Exception {
        // do nothing
    }

    void setMqNativeEndpointConfig(MqNativeEndpointConfig mqNativeEndpointConfig) {
        this.mqNativeEndpointConfig = mqNativeEndpointConfig;
    }
}
