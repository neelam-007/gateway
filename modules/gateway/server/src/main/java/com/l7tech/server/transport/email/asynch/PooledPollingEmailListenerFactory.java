package com.l7tech.server.transport.email.asynch;

import com.l7tech.server.transport.email.PollingEmailListenerFactory;
import com.l7tech.server.transport.email.PollingEmailListener;
import com.l7tech.server.transport.email.EmailListenerConfig;
import com.l7tech.server.transport.email.EmailListenerManager;

/**
 * Creates new PooledPollingEmailListener objects.
 */
public class PooledPollingEmailListenerFactory implements PollingEmailListenerFactory {
    public PollingEmailListener createListener(EmailListenerConfig emailListenerCfg, EmailListenerManager emailListenerManager) {
        return new PooledPollingEmailListenerImpl(emailListenerCfg, emailListenerManager);
    }
}
