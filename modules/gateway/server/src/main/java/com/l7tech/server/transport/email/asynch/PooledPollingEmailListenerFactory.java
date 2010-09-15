package com.l7tech.server.transport.email.asynch;

import com.l7tech.server.transport.email.PollingEmailListenerFactory;
import com.l7tech.server.transport.email.PollingEmailListener;
import com.l7tech.server.transport.email.EmailListenerConfig;
import com.l7tech.server.transport.email.EmailListenerManager;
import com.l7tech.server.ServerConfig;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Creates new PooledPollingEmailListener objects.
 */
public class PooledPollingEmailListenerFactory implements PollingEmailListenerFactory, PropertyChangeListener {
    private ServerConfig serverConfig;
    private final EmailListenerThreadPool emailListenerThreadPool;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000;
    private static final long DEFAULT_TIMEOUT = 60000;
    private static final long MAX_CACHE_AGE_VALUE = 30000;
    private long connectionTimeout;
    private long timeout;

    private static final String IN_CONNECTION_TIMEOUT = "ioMailInConnectTimeout";
    private static final String IN_TIMEOUT = "ioMailInTimeout";

    /**
     * Constructor
     * @param serverConfig  Server config
     * @param emailListenerThreadPool thread pool for email listeners
     */
    public PooledPollingEmailListenerFactory(final ServerConfig serverConfig,
                                             final EmailListenerThreadPool emailListenerThreadPool) {
        if (serverConfig == null) throw new IllegalArgumentException("serverConfig cannot be null.");
        if(emailListenerThreadPool == null) throw new IllegalArgumentException("emailListenerThreadPool cannot be null.");
        this.serverConfig = serverConfig;
        this.emailListenerThreadPool = emailListenerThreadPool;

        //initialize timeouts
        this.connectionTimeout = serverConfig.getTimeUnitPropertyCached(IN_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT, MAX_CACHE_AGE_VALUE);
        this.timeout = serverConfig.getTimeUnitPropertyCached(IN_TIMEOUT, DEFAULT_TIMEOUT, MAX_CACHE_AGE_VALUE);
    }

    @Override
    public PollingEmailListener createListener(EmailListenerConfig emailListenerCfg, EmailListenerManager emailListenerManager) {
        return new PooledPollingEmailListenerImpl(emailListenerCfg, emailListenerManager, emailListenerThreadPool, connectionTimeout, timeout);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (ServerConfig.PARAM_EMAIL_LISTENER_CONNECTION_TIMEOUT.equals(propertyName)) {
            this.connectionTimeout = serverConfig.getTimeUnitPropertyCached(IN_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT, MAX_CACHE_AGE_VALUE);
        } else if (ServerConfig.PARAM_EMAIL_LISTENER_TIMEOUT.equals(propertyName)) {
            this.timeout = serverConfig.getTimeUnitPropertyCached(IN_TIMEOUT, DEFAULT_TIMEOUT, MAX_CACHE_AGE_VALUE);
        }
    }
}
