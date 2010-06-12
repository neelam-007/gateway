package com.l7tech.external.assertions.actional.server;

import com.l7tech.external.assertions.actional.ActionalAssertion;
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
import java.util.concurrent.atomic.AtomicReference;
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
 * MessageReceived (ServerInteraction request)
 * MessageProcessed (ServerInteraction response)
 *
 * @author jules
 */
public class InterceptorEventListener implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(InterceptorEventListener.class.getName());

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

    /**
     * Determines how often cluster properties should be checked for updates.
     * <p/>
     * Default is 2 minutes.
     */
    private static final long POLLING_INTERVAL = 120000;
    // should the above be configurable via a cluster property?

    private AtomicReference<ActionalConfig> configuration =
            new AtomicReference<ActionalConfig>( new ActionalConfig(false, false, false, false, true, DEFAULT_HEADER_NAME, DEFAULT_HEADER_NAME ) );

    /**
     * Determines whether or not this module has been initialized or not.
     */
    private boolean initialized = false;

    /**
     * Create an InterceptorEventListener instance.
     *
     * @param applicationContext the Spring application context.  Required.
     */
    InterceptorEventListener(ApplicationContext applicationContext) {
        this.serverConfig = applicationContext.getBean("serverConfig", ServerConfig.class);
        this.applicationEventProxy = applicationContext.getBean("messageProcessingEventChannel", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(this);
        initialize();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        try {
            final ActionalConfig config = configuration.get();
            if (config.enabled) { //dispatch to appropriate Interceptor handler
                if (event instanceof MessageReceived) {
                    final MessageReceived messageReceivedEvent = (MessageReceived) event;
                    Interceptor.handleServerRequest(messageReceivedEvent, config.transmitProviderPayload, config.enforceInboundTrustZone, config.inboundHttpHeaderName);
                } else if (event instanceof MessageProcessed) {
                    final MessageProcessed messageProcessedEvent = (MessageProcessed) event;
                    Interceptor.handleServerResponse(messageProcessedEvent, config.transmitProviderPayload);
                } else if (event instanceof PreRoutingEvent) {
                    final PreRoutingEvent preRoutingEvent = (PreRoutingEvent) event;
                    Interceptor.handleClientRequest(preRoutingEvent, config.transmitConsumerPayload, config.getHeaderNameIfEnabled());
                } else if (event instanceof PostRoutingEvent) {
                    final PostRoutingEvent postRoutingEvent = (PostRoutingEvent) event;
                    Interceptor.handleClientResponse(postRoutingEvent, config.transmitConsumerPayload);
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
        } catch (InterceptorException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    private void initialize() {
        //check if the module is enabled
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

        final boolean enabledPropertyValue = serverConfig.getBooleanProperty(ClusterProperty.asServerConfigPropertyName(INTERCEPTOR_ENABLE_CLUSTER_PROPERTY), false);
        final boolean transmitProviderPayloadPropertyValue = serverConfig.getBooleanProperty(ClusterProperty.asServerConfigPropertyName(INTERCEPTOR_TRANSMIT_PROVIDER_PAYLOAD_CLUSTER_PROPERTY), false);
        final boolean transmitConsumerPayloadPropertyValue = serverConfig.getBooleanProperty(ClusterProperty.asServerConfigPropertyName(INTERCEPTOR_TRANSMIT_CONSUMER_PAYLOAD_CLUSTER_PROPERTY), false);
        final String configDirPropertyValue = serverConfig.getProperty(ClusterProperty.asServerConfigPropertyName(INTERCEPTOR_CONFIG_DIRECTORY));
        final boolean enforceInboundTrustZoneCheck = serverConfig.getBooleanProperty(ClusterProperty.asServerConfigPropertyName(INTERCEPTOR_ENFORCE_INBOUND_TRUST_ZONE), false);

        final boolean httpHeaderEnabled = serverConfig.getBooleanProperty(ClusterProperty.asServerConfigPropertyName( INTERCEPTOR_ENABLE_OUTBOUND_HTTP_HEADER ), true);
        final String outboundHttpHeaderName = serverConfig.getProperty( ClusterProperty.asServerConfigPropertyName( ActionalAssertion.INTERCEPTOR_HTTP_OUTBOUND_HEADER_NAME ), DEFAULT_HEADER_NAME );
        final String inboundHttpHeaderName = serverConfig.getProperty( ClusterProperty.asServerConfigPropertyName( ActionalAssertion.INTERCEPTOR_HTTP_INBOUND_HEADER_NAME ), DEFAULT_HEADER_NAME );

        configuration.set( new ActionalConfig(
                enabledPropertyValue,
                transmitProviderPayloadPropertyValue,
                transmitConsumerPayloadPropertyValue,
                enforceInboundTrustZoneCheck,
                httpHeaderEnabled,
                outboundHttpHeaderName,
                inboundHttpHeaderName ) );

        //this only takes effect on module re-load
        System.setProperty("com.actional.lg.interceptor.config", configDirPropertyValue);
    }

    boolean isEnabled() {
        return configuration.get().enabled;
    }

    void destroy() throws Exception {
        //destroy some stuff...

        //if module is removed in the middle of processing
        //close out any ServerInteraction or ClientInteraction objects ?

        //shutdown background thread?

        // Unsubscribe from the applicationEventProxy
        applicationEventProxy.removeApplicationListener(this);
    }

    /**
     * Get the current instance, if there is one.
     *
     * @return the current instance, created when onModuleLoaded() was called, or null if there isn't one.
     */
    static InterceptorEventListener getInstance() {
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

    /**
     * The configuration variables map directly to cluster properties.
     * These cluster properties are polled every 'pollingInterval' for changes.
     */
    private static final class ActionalConfig {
        /**
         * Determines whether the Actional Interceptor module has been enabled.
         * <p/>
         * The Interceptor will be enabled/configured via cluster property.
         * <p/>
         */
        private final boolean enabled;

        /**
         * Determines whether or not the Interceptor should transmit the XML payload
         * for either incoming "provider" or outgoing "consumer" requests
         * along with other message processing information to the Actional Agent.
         * <p/>
         */
        private final boolean transmitProviderPayload;
        private final boolean transmitConsumerPayload;

        /**
         * Determines whether or not the Gateway interceptor enforces Trust Zones on inbound messages.
         * If false, TrustZone information will not be checked on the incoming request.  Otherwise TrustZone support will be checked.
         * Note that this setting only affects inbound messages.  TrustZone information will always be included in the outbound actional header.
         */
        private final boolean enforceInboundTrustZone;

        /**
         * Is the outbound HTTP header enabled?
         */
        private final boolean enableHttpHeader;

        /**
         * Name of the HTTP header to check for in requests.
         */
        private final String inboundHttpHeaderName;

        /**
         * Name of the HTTP header to add when routing.
         */
        private final String outboundHttpHeaderName;

        ActionalConfig( final boolean enabled,
                        final boolean transmitProviderPayload,
                        final boolean transmitConsumerPayload,
                        final boolean enforceInboundTrustZone,
                        final boolean enableHttpHeader,
                        final String outboundHttpHeaderName,
                        final String inboundHttpHeaderName ) {
            this.enabled = enabled;
            this.transmitProviderPayload = transmitProviderPayload;
            this.transmitConsumerPayload = transmitConsumerPayload;
            this.enforceInboundTrustZone = enforceInboundTrustZone;
            this.enableHttpHeader = enableHttpHeader;
            this.outboundHttpHeaderName = outboundHttpHeaderName;
            this.inboundHttpHeaderName = inboundHttpHeaderName;
        }

        public String getHeaderNameIfEnabled() {
            return enableHttpHeader ? outboundHttpHeaderName : null;
        }
    }
}
