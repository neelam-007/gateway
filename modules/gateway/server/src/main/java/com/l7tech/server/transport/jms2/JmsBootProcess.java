package com.l7tech.server.transport.jms2;

import com.l7tech.objectmodel.*;
import com.l7tech.server.*;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.server.transport.jms.JmsPropertyMapper;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.server.util.ThreadPoolBean;
import org.springframework.context.ApplicationEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSG Jms2.0 - refactored boot process
 * <br/>
 * A cleaned up version of the boot process using the multi-threaded jms receiver.
 *
 * @author vchan
 */
public class JmsBootProcess extends LifecycleBean implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(JmsBootProcess.class.getName());

    //Thread pool for JMS tasks. Managed by this boot process.
    private final ThreadPoolBean threadPoolBean;

    /** Persisted/configured Jms connection manager */
    private final JmsConnectionManager connectionManager;
    /** Persisted/configured Jms endpoint manager */
    private final JmsEndpointManager endpointManager;
    /** Jms initial context properties */
    private final JmsPropertyMapper jmsPropertyMapper;
    /** Mutex */
    private final Object listenerLock = new Object();
    /** Factory used to instantiate new JmsEndpointListeners */
    private final JmsEndpointListenerFactory jmsEndpointListenerFactory;
    /** Set of all active inbound JMS listeners */
    private Set<JmsEndpointListener> activeListeners = new HashSet<JmsEndpointListener>();
    /** Background timer used to check for connection/endpoint updates */
    private final Timer backgroundTimer;
    /** Boolean flag specifying whether the Jms listeners have been started */
    private boolean started = false;
    /** Is the Gateway ready to process messages */
    private volatile boolean readyForMessages = false;

    // it is expected that these checkers are run on the same thread
    private ConnectionVersionChecker connectionChecker;
    private EndpointVersionChecker endpointChecker;

    /**
     * Constructor.
     *
     * @param threadPoolBean thread pool to manage. This class will start and stop this thread pool bean appropriately.
     * @param licenseManager licence manager
     * @param connectionManager Persisted JMS connection manager
     * @param endpointManager Persisted JMS endpoint manager
     * @param jmsPropertyMapper Jms initial context properties
     * @param jmsEndpointListenerFactory The factory for endpoint listeners
     * @param timer timer object used by the connection/endpoint update checker
     */
    public JmsBootProcess( final ThreadPoolBean threadPoolBean,
                           final LicenseManager licenseManager,
                           final JmsConnectionManager connectionManager,
                           final JmsEndpointManager endpointManager,
                           final JmsPropertyMapper jmsPropertyMapper,
                           final JmsEndpointListenerFactory jmsEndpointListenerFactory,
                           final Timer timer )
    {
        super("JMS Boot Process", logger, GatewayFeatureSets.SERVICE_JMS_MESSAGE_INPUT, licenseManager);

        this.threadPoolBean = threadPoolBean;
        this.connectionManager = connectionManager;
        this.endpointManager = endpointManager;
        this.jmsPropertyMapper = jmsPropertyMapper;
        this.jmsEndpointListenerFactory = jmsEndpointListenerFactory;

        // create a timer if one is not supplied
        if (timer == null)
            this.backgroundTimer = new Timer("JMS config refresh", true);
        else
            this.backgroundTimer = timer;
    }

    /**
     * For handling cluster property changes to the following properties.
     *
     * @param evt the change event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        synchronized(listenerLock) {
            for (JmsEndpointListener listener : activeListeners) {
                listener.propertyChange(evt);
            }
        }
    }

    @Override
    protected void init() {
        if(threadPoolBean == null) throw new IllegalStateException("threadPoolBean is required.");
        if(connectionManager == null) throw new IllegalStateException("connectionManager is required.");
        if(endpointManager == null) throw new IllegalStateException("endpointManager is required.");
        if(jmsEndpointListenerFactory == null) throw new IllegalStateException("jmsEndpointListenerFactory is required.");
    }

    /**
     * Starts {@link JmsEndpointListener}s for the initial configuration.  Also starts the EndpointVersionChecker and ConnectionVersionChecker to periodically
     * check whether endpoints or connections have been created, updated or deleted.
     * <p/>
     * Any exception that is thrown in a JmsEndpointListener's start() method will be logged but not propagated.
     */
    @Override
    protected void doStart() throws LifecycleException {
        if ( !isStarted() ) {
            checkStarted();
        }
    }

    /**
     * Starts all configured JmsEndpointListeners.
     *
     * @throws LifecycleException when problems occur during JMS subsystem startup
     */
    private void startListeners() throws LifecycleException {

        synchronized(listenerLock) {
            if (!started) {
                try {
                    started = true;  // "started" just means that we have already once attempted to start the JMS listener subsystem
                    logger.info("JMS starting.");

                    // Start up listeners for initial configuration
                    Collection<JmsConnection> connections = connectionManager.findAll();
                    List<Goid> staleEndpoints = new ArrayList<Goid>();
                    JmsEndpointConfig endpointCfg;

                    for (JmsConnection connection : connections) {
                        JmsEndpoint[] endpoints = endpointManager.findEndpointsForConnection(connection.getGoid());

                        for (JmsEndpoint endpoint : endpoints) {

                            if (!endpoint.isMessageSource() || endpoint.isDisabled()) continue;

                            endpointCfg = new JmsEndpointConfig(connection, endpoint, this.jmsPropertyMapper, getApplicationContext());
                            endpointCfg.setEvictOnExpired(false);

                            JmsEndpointListener qListener = jmsEndpointListenerFactory.createListener(endpointCfg);

                            try {

                                qListener.start();
                                activeListeners.add(qListener);

                            } catch (LifecycleException e) {
                                logger.log(Level.WARNING, "Couldn't start listener for endpoint " + endpoint
                                        + ".  Will retry periodically", e);
                                staleEndpoints.add(endpoint.getGoid());
                            }
                        }
                    }

                    // create the version checkers
                    connectionChecker = new ConnectionVersionChecker(connectionManager);
                    endpointChecker = new EndpointVersionChecker(endpointManager);

                    for (Goid goid : staleEndpoints) {
                        endpointChecker.markObjectAsStale(goid);
                    }

                } catch ( FindException e ) {
                    String msg = "Couldn't start JMS subsystem!  JMS functionality will be disabled.";
                    logger.log( Level.SEVERE, msg, e );
                    throw new LifecycleException( msg, e );
                }

                // Start periodic check timer
                backgroundTimer.schedule( connectionChecker, connectionChecker.getFrequency() * 2,
                                        connectionChecker.getFrequency() );

                backgroundTimer.schedule( endpointChecker, endpointChecker.getFrequency() * 2,
                                        endpointChecker.getFrequency() );

                logger.info("JMS started.");
            }
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if ( isEventIgnorable(applicationEvent) ) {
            return;
        }

        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof ReadyForMessages) {
            readyForMessages = true;
            checkStarted();
        }
    }

    /**
     * Attempts to stop all running JMS listeners, then stops the EndpointVersionChecker and
     * ConnectionVersionChecker
     */
    @Override
    protected void doStop() {
        synchronized(listenerLock) {

            if (connectionChecker != null)
                connectionChecker.cancel();

            if (endpointChecker != null)
                endpointChecker.cancel();

            for (JmsEndpointListener listener : activeListeners) {
                logger.info("Stopping JMS receiver '" + listener.toString() + "'");
                listener.stop();
            }
            for (JmsEndpointListener listener : activeListeners) {
                logger.info("Waiting for JMS receiver to stop '" + listener.toString() + "'");
                listener.ensureStopped();
            }
            activeListeners.clear();

            threadPoolBean.shutdown();
        }
    }

    private void checkStarted() {
        boolean started;
        synchronized(listenerLock) {
            started = this.started;
        }

        if ( !started ) {
            if ( !readyForMessages ) {
                logger.info("The JMS subsystem will not start until the gateway is ready to process messages.");
            } else if ( isLicensed() ) {
                try {
                    threadPoolBean.start();
                    startListeners();
                } catch (LifecycleException e) {
                    logger.log(Level.SEVERE, "Unable to start JMS EndpointListener", e);
                }
            }
        }
    }

    private boolean isEventIgnorable(ApplicationEvent applicationEvent) {
        return applicationEvent instanceof AuditDetailEvent ||
                applicationEvent instanceof MessageProcessed ||
                applicationEvent instanceof FaultProcessed;
    }


    /**
     * Handles the event fired by the deletion of a JmsConnection.
     */
    private void connectionDeleted( Goid deletedConnectionGoid ) {
        synchronized(listenerLock) {

            JmsEndpointListener listener;
            for (Iterator<JmsEndpointListener> i = activeListeners.iterator(); i.hasNext();) {

                listener = i.next();

                if (listener.getJmsConnectionGoid().equals(deletedConnectionGoid)) {
                    listener.stop();
                    i.remove();
                }
            }
        }
    }

    /**
     * Handles the event fired by the update of a JmsConnection.
     */
    private void connectionUpdated(JmsConnection updatedConnection) {
        synchronized(listenerLock) {
            Goid updatedGoid = updatedConnection.getGoid();

            try {
                EntityHeader[] endpoints = endpointManager.findEndpointHeadersForConnection( updatedGoid );
                for (EntityHeader header : endpoints) {
                    JmsEndpoint endpoint = endpointManager.findByPrimaryKey(header.getGoid());
                    endpointChecker.notifyUpdate(endpoint.getGoid(), endpoint.getVersion());
                    endpointUpdated(endpoint, false);
                }
            } catch ( FindException e ) {
                logger.log( Level.SEVERE, "Caught exception finding endpoints for a connection!", e );
            }
        }
    }

    /**
     * Handles the event generated by the discovery of the deletion of a JmsEndpoint.
     *
     * @param deletedEndpointGoid the GOID of the endpoint that has been deleted.
     */
    private void endpointDeleted( Goid deletedEndpointGoid ) {
        synchronized(listenerLock) {
            JmsEndpointListener listener;
            for (Iterator<JmsEndpointListener> i = activeListeners.iterator(); i.hasNext();) {
                listener = i.next();
                if (listener.getJmsEndpointGoid().equals(deletedEndpointGoid) ) {
                    listener.stop();
                    i.remove();
                }
            }
        }
    }


    /**
     * Handles the event fired by the update or creation of a JmsEndpoint.
     *
     * Calls endpointDeleted to shut down any receiver(s) that might already
     * be listening to that endpoint.
     *
     * @param updatedEndpoint the JmsEndpoint that has been created or updated.
     */
    private void endpointUpdated( JmsEndpoint updatedEndpoint, boolean notify ) {
        // Stop any existing receivers for this endpoint
        endpointDeleted( updatedEndpoint.getGoid() );

        if (updatedEndpoint.isMessageSource() && !updatedEndpoint.isDisabled()) {
            JmsEndpointListener newListener = null;
            try {
                JmsConnection connection = connectionManager.findByPrimaryKey( updatedEndpoint.getConnectionGoid() );
                if (notify)
                    connectionChecker.notifyUpdate(connection.getGoid(), connection.getVersion());

                JmsEndpointConfig newEndpointCfg = new JmsEndpointConfig(connection, updatedEndpoint, jmsPropertyMapper, getApplicationContext());
                newListener = jmsEndpointListenerFactory.createListener(newEndpointCfg);
                newListener.start();
                synchronized(listenerLock) {
                    activeListeners.add(newListener);
                }
            } catch (LifecycleException e) {
                logger.warning("Exception while initializing receiver " + newListener +
                                "; will try again later: " + e.toString());
                synchronized(listenerLock) {
                    if (endpointChecker != null)
                        endpointChecker.markObjectAsStale( updatedEndpoint.getGoid() );
                }
            } catch ( FindException e ) {
                logger.log( Level.SEVERE, "Couldn't find connection for endpoint " + updatedEndpoint, e );
            }
        }
    }

    /**
     * Periodically checks for new, updated or deleted JMS endpoints
     */
    private class EndpointVersionChecker extends PeriodicVersionCheck {
        EndpointVersionChecker( JmsEndpointManager mgr ) throws FindException {
            super( mgr );
        }

        @Override
        protected void onDelete( Goid removedGoid ) {
            logger.info( "Endpoint " + removedGoid + " deleted!" );
            endpointDeleted( removedGoid );
        }

        @Override
        protected void onUpdate( PersistentEntity updatedEntity ) {
            logger.info( "Endpoint " + updatedEntity.getGoid() + " updated!" );
            endpointUpdated( (JmsEndpoint)updatedEntity, true );
        }

        @Override
        protected void onCreate( PersistentEntity createdEntity ) {
            logger.info( "Endpoint " + createdEntity.getGoid() + " created!" );
            endpointUpdated( (JmsEndpoint)createdEntity, true );
        }

        @Override
        public boolean notifyUpdate(Goid updatedGoid, int version) {
            return super.notifyUpdate(updatedGoid, version);
        }
    }

    /**
     * Periodically checks for new, updated or deleted JMS connections
     */
    private class ConnectionVersionChecker extends PeriodicVersionCheck {
        ConnectionVersionChecker( JmsConnectionManager mgr ) throws FindException {
            super(mgr);
        }

        @Override
        protected void onDelete( Goid removedGoid ) {
            logger.info( "Connection " + removedGoid + " deleted!" );
            connectionDeleted( removedGoid );
        }

        @Override
        protected void onUpdate( PersistentEntity updatedEntity ) {
            logger.info( "Connection " + updatedEntity.getGoid() + " updated!" );
            connectionUpdated( (JmsConnection)updatedEntity );
        }

        @Override
        protected void onCreate(PersistentEntity createdEntity) {
            logger.info( "Connection " + createdEntity.getGoid() + " created!" );
            connectionUpdated( (JmsConnection)createdEntity );
        }

        @Override
        public boolean notifyUpdate(Goid updatedOid, int version) {
            return super.notifyUpdate(updatedOid, version);
        }
    }

}