package com.l7tech.server;

import com.l7tech.server.event.system.ReadyForMessages;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple bean that keeps track of what state the Gateway is currently in.
 */
public class GatewayState implements ApplicationListener, Ordered {
    private final AtomicBoolean readyForMessages = new AtomicBoolean(false);

    /**
     * @return true if the ReadyForMessages application event has already been seen in this application context.
     */
    public boolean isReadyForMessages() {
        return readyForMessages.get();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ReadyForMessages) {
            readyForMessages.set(true);
        }
    }

    @Override
    public int getOrder() {
        return -10000;
    }
}
