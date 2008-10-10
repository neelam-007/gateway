package com.l7tech.server.transport.email;

import com.l7tech.server.transport.email.asynch.PooledPollingEmailListenerImpl;
import com.l7tech.gateway.common.transport.email.EmailListener;
import org.springframework.context.ApplicationContext;

import javax.mail.internet.MimeMessage;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper for EmailListener objects, that also contains a reference to the application context.
 */
public class EmailListenerConfig {
    private final EmailListener emailListener;
    /** Spring application context */
    private final ApplicationContext appContext;
    /** Maximum email message size allowed */
    private final AtomicInteger maxMessageSize = new AtomicInteger(PooledPollingEmailListenerImpl.DEFAULT_MAX_SIZE);

    /**
     * Constructor.
     *
     * @param emailListener configured email listener
     * @param appContext spring application context
     */
    public EmailListenerConfig(final EmailListener emailListener,
                               final ApplicationContext appContext)
    {
        super();
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
    public int getMaxMessageSize() {
        return maxMessageSize.intValue();
    }

    /**
     * Sets the max message size property.
      * @param newValue the new value to set
     */
    public void setMessageMaxSize(int newValue) {
        maxMessageSize.set(newValue);
    }
}
