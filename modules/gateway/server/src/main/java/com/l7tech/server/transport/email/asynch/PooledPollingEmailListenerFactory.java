package com.l7tech.server.transport.email.asynch;

import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.transport.email.PollingEmailListenerFactory;
import com.l7tech.server.transport.email.PollingEmailListener;
import com.l7tech.server.transport.email.EmailListenerConfig;
import com.l7tech.server.transport.email.EmailListenerManager;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.Config;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Creates new PooledPollingEmailListener objects.
 */
public class PooledPollingEmailListenerFactory implements PollingEmailListenerFactory, PropertyChangeListener {
    private Config config;
    private final ThreadPoolBean threadPoolBean;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000;
    private static final long DEFAULT_TIMEOUT = 60000;
    private static final long MAX_CACHE_AGE_VALUE = 30000;
    private long connectionTimeout;
    private long timeout;

    private static final String IN_CONNECTION_TIMEOUT = "ioMailInConnectTimeout";
    private static final String IN_TIMEOUT = "ioMailInTimeout";

    /**
     * Constructor
     * @param config  Server config
     * @param threadPoolBean thread pool bean for email listeners
     */
    public PooledPollingEmailListenerFactory(final Config config,
                                             final ThreadPoolBean threadPoolBean) {
        if ( config == null) throw new IllegalArgumentException("serverConfig cannot be null.");
        if(threadPoolBean == null) throw new IllegalArgumentException("threadPoolBean cannot be null.");
        this.config = config;
        this.threadPoolBean = threadPoolBean;

        //initialize timeouts
        this.connectionTimeout = config.getTimeUnitProperty( IN_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT );
        this.timeout = config.getTimeUnitProperty( IN_TIMEOUT, DEFAULT_TIMEOUT );
    }

    @Override
    public PollingEmailListener createListener(EmailListenerConfig emailListenerCfg, EmailListenerManager emailListenerManager) {
        return new PooledPollingEmailListenerImpl(emailListenerCfg, emailListenerManager, threadPoolBean, connectionTimeout, timeout);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if ( ServerConfigParams.PARAM_EMAIL_LISTENER_CONNECTION_TIMEOUT.equals(propertyName)) {
            this.connectionTimeout = config.getTimeUnitProperty( IN_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT );
        } else if ( ServerConfigParams.PARAM_EMAIL_LISTENER_TIMEOUT.equals(propertyName)) {
            this.timeout = config.getTimeUnitProperty( IN_TIMEOUT, DEFAULT_TIMEOUT );
        }
    }
}
