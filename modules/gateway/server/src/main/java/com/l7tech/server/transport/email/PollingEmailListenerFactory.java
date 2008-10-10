package com.l7tech.server.transport.email;

import com.l7tech.gateway.common.transport.email.EmailListener;

/**
 * An interface for objects that can create PollingEmailListener objects.
 */
public interface PollingEmailListenerFactory {
    public PollingEmailListener createListener(EmailListenerConfig emailListenerCfg, EmailListenerManager emailListenerManager);
}
