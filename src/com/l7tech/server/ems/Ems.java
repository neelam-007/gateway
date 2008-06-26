package com.l7tech.server.ems;

import com.l7tech.common.util.ExceptionUtils;
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
                "com/l7tech/server/ems/resources/restletContext.xml",
        });
        if (!appContext.compareAndSet(null, newAppContext))
            throw new IllegalStateException("EMS already started");
    }

    /**
     * Stops the Enterprise Manager Server and returns once it has been stopped.
     */
    public void stop() {
        try {
            final AbstractApplicationContext ac = appContext.getAndSet(null);
            if (ac != null)
                ac.destroy();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error while stopping ApplicationContext: " + ExceptionUtils.getMessage(t), t);
        }
    }
}
