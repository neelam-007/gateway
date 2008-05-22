package com.l7tech.server.ems;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SyspropUtil;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.resource.Resource;
import org.springframework.context.ApplicationContext;
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
    private static final int HTTP_PORT = SyspropUtil.getInteger("com.l7tech.ems.httpPort", 8182);
    private static final int HTTPS_PORT = SyspropUtil.getInteger("com.l7tech.ems.httpsPort", 8545);

    private final AtomicReference<Component> component = new AtomicReference<Component>();
    private final AtomicReference<AbstractApplicationContext> appContext = new AtomicReference<AbstractApplicationContext>();

    /**
     * Starts the Enterprise Manager Server and returns once it has been started.
     *
     * @throws Exception if the server fails to start.
     */
    public void start() throws Exception {
        final Component c = new Component();
        if (!component.compareAndSet(null, c))
            throw new IllegalStateException("EMS already started");

        c.getLogService().setLoggerName(logger.getName());
        c.getServers().add(Protocol.HTTP, HTTP_PORT);
        c.getClients().add(Protocol.CLAP);

        appContext.set(new ClassPathXmlApplicationContext(
                "com/l7tech/server/ems/resources/emsApplicationContext.xml"
        ));

        c.getDefaultHost().attach(new EmsApplication(c.getContext(), appContext.get()));
        c.start();
    }

    /**
     * Stops the Enterprise Manager Server and returns once it has been stopped.
     */
    public void stop() {
        stopComponent();
        stopAppContext();
    }

    /**
     * Get the Spring ApplicationContext associated with the specified restlet Resource.
     *
     * @param resource the resource to look up. Required.
     * @return the ApplicationContext for this resource.  Never null.
     * @throws IllegalArgumentException if the specified resource does not have an EmsApplication or ApplicationContext
     *         associated with it.
     */
    public static ApplicationContext getAppContext(Resource resource) throws IllegalArgumentException {
        ApplicationContext appContext = getEmsApp(resource).getSpringContext();
        if (appContext == null)
            throw new IllegalArgumentException("No ApplicationContext is available for the specified Restlet Application");
        return appContext;
    }

    /**
     * Get the EmsApplication instance associated with the specified restlet Resource.
     *
     * @param resource the resource to look up.  Required.
     * @return the EmsApplication instance for this Resource.  Never null.
     * @throws IllegalArgumentException if the specified resource does not have an EmsApplication
     *         associated with it.
     */
    public static EmsApplication getEmsApp(Resource resource) throws IllegalArgumentException {
        Application app = resource.getApplication();
        if (!(app instanceof EmsApplication))
            throw new IllegalArgumentException("No ApplicationContext is available for the specified Restlet Application");
        return (EmsApplication)app;
    }

    private void stopComponent() {
        try {
            final Component c = component.getAndSet(null);
            if (c != null)
                c.stop();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error while stopping Component: " + ExceptionUtils.getMessage(t), t);
        }
    }

    private void stopAppContext() {
        try {
            final AbstractApplicationContext ac = appContext.getAndSet(null);
            if (ac != null)
                ac.destroy();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error while stopping ApplicationContext: " + ExceptionUtils.getMessage(t), t);
        }
    }
}
