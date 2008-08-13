package com.l7tech.server.ems;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.event.system.Stopped;
import com.l7tech.gateway.common.Component;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a running Enterprise Manager Server instance, including its HTTP listeners, database, Spring
 * context, and everything else.
 */
public class Ems {
    private static final Logger logger = Logger.getLogger(Ems.class.getName());

    private final AtomicReference<AbstractApplicationContext> appContext = new AtomicReference<AbstractApplicationContext>();

    /**
     * Starts the Enterprise Manager Server and returns once it has been started.
     *
     * @throws Exception if the server fails to start.
     */
    public void start() throws Exception {
        if (appContext.get() != null)
            throw new IllegalStateException("EMS already started");
        AbstractApplicationContext newAppContext = new ClassPathXmlApplicationContext(new String[] {
                "com/l7tech/server/ems/resources/emsApplicationContext.xml",
        });
        if (!appContext.compareAndSet(null, newAppContext))
            throw new IllegalStateException("EMS already started");

        newAppContext.publishEvent(new Started(this, Component.ENTERPRISE_MANAGER, "127.0.0.1")); //TODO [steve] EMS IP
    }

    /**
     * Stops the Enterprise Manager Server and returns once it has been stopped.
     */
    public void stop() {
        try {
            final AbstractApplicationContext ac = appContext.getAndSet(null);
            if (ac != null) {
                ac.publishEvent(new Stopped(this, Component.ENTERPRISE_MANAGER, "127.0.0.1")); //TODO [steve] EMS IP                
                ac.destroy();
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error while stopping ApplicationContext: " + ExceptionUtils.getMessage(t), t);
        }
    }
}
