package com.l7tech.server.transport.email;

import com.l7tech.server.transport.email.asynch.PooledPollingEmailListenerImpl;
import com.l7tech.gateway.common.transport.email.EmailListener;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A wrapper for EmailListener objects, that also contains a reference to the application context.
 */
public class EmailListenerConfig {
    private final EmailListener emailListener;
    /** Spring application context */
    private final ApplicationContext appContext;
    /** Maximum email message size allowed */
    private final AtomicLong maxMessageSize = new AtomicLong(PooledPollingEmailListenerImpl.DEFAULT_MAX_SIZE);

    /**
     * Constructor.
     *
     * @param emailListener configured email listener
     * @param appContext spring application context
     */
    public EmailListenerConfig(final EmailListener emailListener,
                               final ApplicationContext appContext)
    {
        this.emailListener = emailListener;
        this.appContext = appContext;
    }

    /* Getters */
    public ApplicationContext getApplicationContext() {
        return appContext;
    }

    public EmailListener getEmailListener() {
        return emailListener;
    }

    /**
     * Returns the max message size.
     * @return int value
     */
    public long getMaxMessageSize() {
        return maxMessageSize.get();
    }

    /**
     * Sets the max message size property.
      * @param newValue the new value to set
     */
    public void setMessageMaxSize(long newValue) {
        maxMessageSize.set(newValue);
    }
}
