package com.l7tech.server.module.actional;

import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Set;
import java.util.TimerTask;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.policy.AssertionModuleRegistrationEvent;
import com.l7tech.server.event.*;
import com.l7tech.server.event.system.Starting;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Background;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.cluster.ClusterProperty;
import com.l7tech.external.assertions.actional.ActionalAssertion;

/**
 * A class that listens on Actional Interceptor specific events
 * and dispatches to the appropriate {@link Interceptor} handler.
 * <p/>
 * Actional Interceptor specific events are:
 * PreRoutingEvent (ClientInteraction request)
 * PostRoutingEvent (ClientInteraction response)
 * MessageRecieved (ServerInteraction request)
 * MessageProcessed (ServerInteraction response)
 *
 * @author jules
 */
public class InterceptorEventListener implements ApplicationListener {
    protected static final Logger logger = Logger.getLogger(InterceptorEventListener.class.getName());

    /**
     * This module's instance of the InterceptorEventListener.
     */
    private static InterceptorEventListener instance = null;

    /**
     * Used for cluster property management
     */
    private final ServerConfig serverConfig;

    /**
     * Used to subscribe/unsubscribe to application events.
     */
    private final ApplicationEventProxy applicationEventProxy;

    // CLUSTER PROPERTIES STUFF
    /**
     * Determines how often cluster properties should be checked for updates.
     * <p/>
     * Default is 2 minutes.
     */
    private static final long POLLING_INTERVAL = 120000;
    // should the above be configurable via a cluster property?

    /**
     * The following variables map directly to cluster properties.
     * These cluster properties are polled every 'pollingInterval' for changes.
     */

    /**
     * Determines whether the Actional Interceptor module has been enabled.
     * <p/>
     * The Interceptor will be enabled/configured via cluster property.
     * <p/>
     */
    private boolean enabled = false;

    /**
     * Deteremines whether or not the Interceptor should transmit the XML payload
     * for either incoming "provider" or outgoing "consumer" requests
     * along with other message processing information to the Actional Agent.
     * <p/>
     */
    private boolean transmitProviderPayload = false;
    private boolean transmitConsumerPayload = false;

    // END CLUSTER PROPERTIES STUFF

    /**
     * Determines whether or not this module has been initialized or not.
     */
    private boolean initialized = false;

    /**
     * Create an InterceptorEventListener instance.
     *
     * @param applicationContext the Spring application context.  Required.
     */
    public InterceptorEventListener(ApplicationContext applicationContext) {
        this.serverConfig = (ServerConfig) applicationContext.getBean("serverConfig", ServerConfig.class);
        this.applicationEventProxy = (ApplicationEventProxy) applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(this);

        initialize();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (enabled) {//dispatch to appropriate Interceptor handler
            if (event instanceof MessageReceived) {
                final MessageReceived messageReceivedEvent = (MessageReceived) event;
                Interceptor.handleServerRequest(messageReceivedEvent, transmitProviderPayload);
            } else if (event instanceof MessageProcessed) {
                final MessageProcessed messageProcessedEvent = (MessageProcessed) event;
                Interceptor.handleServerResponse(messageProcessedEvent, transmitProviderPayload);
            } else if (event instanceof PreRoutingEvent) {
                final PreRoutingEvent preRoutingEvent = (PreRoutingEvent) event;
                Interceptor.handleClientRequest(preRoutingEvent, transmitConsumerPayload);
            } else if (event instanceof PostRoutingEvent) {
                final PostRoutingEvent postRoutingEvent = (PostRoutingEvent) event;
                Interceptor.handleClientResponse(postRoutingEvent, transmitConsumerPayload);
            }
        } else if (!initialized) {
            if (event instanceof AssertionModuleRegistrationEvent) {// TODO do we actually care about any other events?
                //since all our init is done onModuleLoaded()
                logger.log(Level.FINEST, "Interceptor received AssertionModuleRegistrationEvent.");
                //a module (.aar file) has been registered
                AssertionModuleRegistrationEvent moduleRegistrationEvent = (AssertionModuleRegistrationEvent) event;
                Set<? extends Assertion> prototypes = moduleRegistrationEvent.getModule().getAssertionPrototypes();
                if (!prototypes.isEmpty()) {//should never be empty - this module should always only have 1 assertion prototype
                    Assertion prototype = prototypes.iterator().next();
                    //check if this new modular assertion is ours
                    if (prototype.getClass().getClassLoader() == this.getClass().getClassLoader()) {
                        initialize();
                    }
                }
            } else if (event instanceof Starting || event instanceof Started) {
                logger.log(Level.FINEST, "Interceptor received Starting OR Started event.");
                //do we need to capture these events...why not wait until the registration event (above)?

                //gateway is booting & our module is loaded
                initialize();
            }
        }
    }

    private void initialize() {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Actional Interceptor module successfully initialized.");
        }

        //check if the modul then e is enabled
        updateClusterProperties();

        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                updateClusterProperties();
            }
        }, POLLING_INTERVAL, POLLING_INTERVAL);

        initialized = true;
    }

    /**
     * Updates the cluster property values.
     * <p/>
     * This method is called by a background timer task every 120 seconds.
     */
    private void updateClusterProperties() {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Updating Actional Interceptor cluster property values...");
        }

        String enabledPropertyValue = serverConfig.getProperty(ClusterProperty.asServerConfigPropertyName(ActionalAssertion.INTERCEPTOR_ENABLE_CLUSTER_PROPERTY));
        String transmitProviderPayloadPropertyValue = serverConfig.getProperty(ClusterProperty.asServerConfigPropertyName(ActionalAssertion.INTERCEPTOR_TRANSMIT_PROVIDER_PAYLOAD_CLUSTER_PROPERTY));
        String transmitConsumerPayloadPropertyValue = serverConfig.getProperty(ClusterProperty.asServerConfigPropertyName(ActionalAssertion.INTERCEPTOR_TRANSMIT_CONSUMER_PAYLOAD_CLUSTER_PROPERTY));

        synchronized (this) {
            enabled = Boolean.parseBoolean(enabledPropertyValue);
            transmitProviderPayload = Boolean.parseBoolean(transmitProviderPayloadPropertyValue);
            transmitConsumerPayload = Boolean.parseBoolean(transmitProviderPayloadPropertyValue);
        }
    }

    public void destroy() throws Exception {
        try {
            //destroy some stuff...

            //if module is removed in the middle of processing
            //close out any ServerInteraction or ClientInteraction objects ?

            //shutdown background thread?

        } finally {
            // Unsubscribe from the applicationEventProxy
            if (applicationEventProxy != null)
                applicationEventProxy.removeApplicationListener(this);
        }
    }

    /**
     * Get the current instance, if there is one.
     *
     * @return the current instance, created when onModuleLoaded() was called, or null if there isn't one.
     */
    public static InterceptorEventListener getInstance() {
        return instance;
    }

    /*
     * Called by the ServerAssertionRegistry when the module containing this class is first loaded
     */
    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "Interceptor module is already initialized.");
        } else {
            instance = new InterceptorEventListener(context);
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.INFO, "Interceptor module is shutting down");
            try {
                instance.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Interceptor module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                instance = null;
            }
        }
    }
}
