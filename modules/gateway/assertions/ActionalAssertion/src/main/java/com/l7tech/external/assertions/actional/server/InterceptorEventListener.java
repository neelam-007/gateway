package com.l7tech.external.assertions.actional.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.MessageReceived;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.event.system.Starting;
import com.l7tech.server.policy.AssertionModuleRegistrationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.actional.ActionalAssertion.*;

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
    private AtomicBoolean enabled = new AtomicBoolean(false);

    /**
     * Deteremines whether or not the Interceptor should transmit the XML payload
     * for either incoming "provider" or outgoing "consumer" requests
     * along with other message processing information to the Actional Agent.
     * <p/>
     */
    private AtomicBoolean transmitProviderPayload = new AtomicBoolean(false);
    private AtomicBoolean transmitConsumerPayload = new AtomicBoolean(false);

    /**
     * Determines whether or not the Gateway interceptor enforces Trust Zones on inbound messages.
     * If false, TrustZone information will not be checked on the incoming request.  Otherwise TrustZone support will be checked.
     * Note that this setting only affects inbound messages.  TrustZone information will always be included in the outbound actional header.
     */
    private AtomicBoolean enforceInboundTrustZone = new AtomicBoolean(false);
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
        this.applicationEventProxy = (ApplicationEventProxy) applicationContext.getBean("messageProcessingEventChannel", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(this);
        initialize();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (enabled.get()) {//dispatch to appropriate Interceptor handler
            if (event instanceof MessageReceived) {
                final MessageReceived messageReceivedEvent = (MessageReceived) event;
                Interceptor.handleServerRequest(messageReceivedEvent, transmitProviderPayload.get(), enforceInboundTrustZone.get());
            } else if (event instanceof MessageProcessed) {
                final MessageProcessed messageProcessedEvent = (MessageProcessed) event;
                Interceptor.handleServerResponse(messageProcessedEvent, transmitProviderPayload.get());
            } else if (event instanceof PreRoutingEvent) {
                final PreRoutingEvent preRoutingEvent = (PreRoutingEvent) event;
                Interceptor.handleClientRequest(preRoutingEvent, transmitConsumerPayload.get());
            } else if (event instanceof PostRoutingEvent) {
                final PostRoutingEvent postRoutingEvent = (PostRoutingEvent) event;
                Interceptor.handleClientResponse(postRoutingEvent, transmitConsumerPayload.get());
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
        //check if the modul then e is enabled
        updateClusterProperties();

        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                updateClusterProperties();
            }
        }, POLLING_INTERVAL, POLLING_INTERVAL);

        initialized = true;

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Actional Interceptor module successfully initialized.");
        }
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

        boolean enabledPropertyValue = Boolean.parseBoolean(serverConfig.getProperty(ClusterProperty.asServerConfigPropertyName(INTERCEPTOR_ENABLE_CLUSTER_PROPERTY)));
        boolean transmitProviderPayloadPropertyValue = Boolean.parseBoolean(serverConfig.getProperty(ClusterProperty.asServerConfigPropertyName(INTERCEPTOR_TRANSMIT_PROVIDER_PAYLOAD_CLUSTER_PROPERTY)));
        boolean transmitConsumerPayloadPropertyValue = Boolean.parseBoolean(serverConfig.getProperty(ClusterProperty.asServerConfigPropertyName(INTERCEPTOR_TRANSMIT_CONSUMER_PAYLOAD_CLUSTER_PROPERTY)));
        String configDirPropertyValue = serverConfig.getProperty(ClusterProperty.asServerConfigPropertyName(INTERCEPTOR_CONFIG_DIRECTORY));
        boolean enforceInboundTrustZoneCheck = Boolean.parseBoolean(serverConfig.getProperty(ClusterProperty.asServerConfigPropertyName(INTERCEPTOR_ENFORCE_INBOUND_TRUST_ZONE)));

        synchronized (this) {
            enabled.set(enabledPropertyValue);
            transmitProviderPayload.set(transmitProviderPayloadPropertyValue);
            transmitConsumerPayload.set(transmitConsumerPayloadPropertyValue);
            enforceInboundTrustZone.set(enforceInboundTrustZoneCheck);
        }

        //this only takes effect on module re-load
        System.setProperty("com.actional.lg.interceptor.config", configDirPropertyValue);
    }

    public boolean isEnabled() {
        return enabled.get();
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
