/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id: JmsBootProcess.java 18643 2008-04-17 20:50:15Z megery $
 */

package com.l7tech.server.transport.jms2;

import com.l7tech.common.LicenseManager;
import com.l7tech.common.audit.AuditDetailEvent;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.*;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.server.transport.jms.JmsPropertyMapper;
import com.l7tech.server.transport.jms2.asynch.JmsThreadPool;
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

    /** Server configuration properties */
    private final ServerConfig serverConfig;
    /** Persisted/configured Jms connection manager */
    private final JmsConnectionManager connectionManager;
    /** Persisted/configured Jms endpoint manager */
    private final JmsEndpointManager endpointManager;
    /** Jms initial context properties */
    private final JmsPropertyMapper jmsPropertyMapper;
    /** Mutex */
    private final Object listenerLock = new Object();
    /** Factory used to instantiate new JmsEndpointListeners */
    private JmsEndpointListenerFactory jmsEndpointListenerFactory;
    /** Set of all active inbound JMS listeners */
    private Set<JmsEndpointListener> activeListeners = new HashSet<JmsEndpointListener>();
    /** Background timer used to check for connection/endpoint updates */
    private final Timer backgroundTimer;
    /** Boolean flag specifying whether the Jms listeners have been started */
    private boolean started = false;

    // it is expected that these checkers are run on the same thread
    private ConnectionVersionChecker connectionChecker;
    private EndpointVersionChecker endpointChecker;

    /**
     * Constructor.  Remains unchanged from original constructor to us to swap between
     * the "legacy" and new JMS implementations.
     *
     * @param serverConfig configuration properties
     * @param licenseManager licence manager
     * @param connectionManager Persisted JMS connection manager
     * @param endpointManager Persisted JMS endpoint manager
     * @param jmsPropertyMapper Jms intital context properties
     * @param timer timer object used by the connection/endpoint update checker
     */
    public JmsBootProcess(ServerConfig serverConfig,
                          LicenseManager licenseManager,
                          JmsConnectionManager connectionManager,
                          JmsEndpointManager endpointManager,
                          JmsPropertyMapper jmsPropertyMapper,
                          Timer timer)
    {
        super("JMS Boot Process", logger, GatewayFeatureSets.SERVICE_JMS_MESSAGE_INPUT, licenseManager);

        this.serverConfig = serverConfig;
        this.connectionManager = connectionManager;
        this.endpointManager = endpointManager;
        this.jmsPropertyMapper = jmsPropertyMapper;

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
        if(serverConfig == null) throw new IllegalStateException("serverConfig is required.");
        if(connectionManager == null) throw new IllegalStateException("connectionManager is required.");
        if(endpointManager == null) throw new IllegalStateException("endpointManager is required.");
    }

    /**
     * Starts {@link com.l7tech.server.transport.jms.JmsReceiver}s for the initial configuration.  Also starts the EndpointVersionChecker and ConnectionVersionChecker to periodically
     * check whether endpoints or connections have been created, updated or deleted.
     * <p/>
     * Any exception that is thrown in a JmsReceiver's start() method will be logged but not propagated.
     */
    @Override
    protected void doStart() throws LifecycleException {
        if (isStarted())
            return;
        logger.info("The JMS subsystem will not start until the gateway is ready to process messages.");
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
                    List<Long> staleEndpoints = new ArrayList<Long>();
                    JmsEndpointConfig endpointCfg = null;

                    for (JmsConnection connection : connections) {
                        JmsEndpoint[] endpoints = endpointManager.findEndpointsForConnection(connection.getOid());

                        for (JmsEndpoint endpoint : endpoints) {

                            if (!endpoint.isMessageSource() || endpoint.isDisabled()) continue;

                            endpointCfg = new JmsEndpointConfig(connection, endpoint, this.jmsPropertyMapper, getApplicationContext());

//                            JmsReceiver receiver = new JmsReceiver(connection, endpoint, endpoint.getReplyType(), jmsPropertyMapper, getApplicationContext());
                            // need to check to see if it's Queue-based or pub/sub (topic)?
                            JmsEndpointListener qListener = jmsEndpointListenerFactory.createListener(endpointCfg);

                            try {

                                qListener.start();
                                activeListeners.add(qListener);

                            } catch (LifecycleException e) {
                                logger.log(Level.WARNING, "Couldn't start listener for endpoint " + endpoint
                                        + ".  Will retry periodically", e);
                                staleEndpoints.add(endpoint.getOid());
                            }
                        }
                    }

                    // create the version checkers
                    connectionChecker = new ConnectionVersionChecker(connectionManager);
                    endpointChecker = new EndpointVersionChecker(endpointManager);

                    for (Long oid : staleEndpoints) {
                        endpointChecker.markObjectAsStale(oid);
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

        if (!isStarted())
            return;

        if (applicationEvent instanceof ReadyForMessages) {
            try {
                startListeners();
            } catch (LifecycleException e) {
                logger.log(Level.SEVERE, "Unable to start JMS EndpointListener", e);
            }
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

            // shutdown the JmsThreadPool (whether it's used or not)
            JmsThreadPool.getInstance().shutdown();
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
    private void connectionDeleted( long deletedConnectionOid ) {
        synchronized(listenerLock) {

            /*
             * TODO: need to implement
             */
            JmsEndpointListener listener = null;
            for (Iterator<JmsEndpointListener> i = activeListeners.iterator(); i.hasNext();) {

                listener = i.next();

                if (listener.getJmsConnectionOid() == deletedConnectionOid ) {
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

            /*
             * TODO: need to implement
             */

            long updatedOid = updatedConnection.getOid();

            try {
                EntityHeader[] endpoints = endpointManager.findEndpointHeadersForConnection( updatedOid );
                for (EntityHeader header : endpoints) {
                    JmsEndpoint endpoint = endpointManager.findByPrimaryKey(header.getOid());
                    endpointChecker.notifyUpdate(endpoint.getOid(), endpoint.getVersion());
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
     * @param deletedEndpointOid the OID of the endpoint that has been deleted.
     */
    private void endpointDeleted( long deletedEndpointOid ) {
        synchronized(listenerLock) {

            /*
             * TODO: need to implement
             */
            JmsEndpointListener listener = null;
            for (Iterator<JmsEndpointListener> i = activeListeners.iterator(); i.hasNext();) {
                listener = i.next();
                if (listener.getJmsEndpointOid() == deletedEndpointOid ) {
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

        /*
         * TODO: need to implement
         */
        // Stop any existing receivers for this endpoint
        endpointDeleted( updatedEndpoint.getOid() );

        if (updatedEndpoint.isMessageSource()) {
            if (updatedEndpoint.isDisabled()) return;
            JmsEndpointListener newListener = null;
            try {
                JmsConnection connection = connectionManager.findByPrimaryKey( updatedEndpoint.getConnectionOid() );
                if (notify)
                    connectionChecker.notifyUpdate(connection.getOid(), connection.getVersion());

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
                        endpointChecker.markObjectAsStale( updatedEndpoint.getOid() );
                }
            } catch ( FindException e ) {
                logger.log( Level.SEVERE, "Couldn't find connection for endpoint " + updatedEndpoint, e );
            }
        }
    }


    /**
     * Sets the JmsEndpointListenerFactory.
     *
     * @param newFactory the new factory
     */
    public void setJmsEndpointListenerFactory(JmsEndpointListenerFactory newFactory) {
        this.jmsEndpointListenerFactory = newFactory;
    }


    /**
     * Periodically checks for new, updated or deleted JMS endpoints
     */
    private class EndpointVersionChecker extends PeriodicVersionCheck {
        EndpointVersionChecker( JmsEndpointManager mgr ) throws FindException {
            super( mgr );
        }

        @Override
        protected void onDelete( long removedOid ) {
            logger.info( "Endpoint " + removedOid + " deleted!" );
            endpointDeleted( removedOid );
        }

        @Override
        protected void onUpdate( PersistentEntity updatedEntity ) {
            logger.info( "Endpoint " + updatedEntity.getOid() + " updated!" );
            endpointUpdated( (JmsEndpoint)updatedEntity, true );
        }

        @Override
        protected void onCreate( PersistentEntity createdEntity ) {
            logger.info( "Endpoint " + createdEntity.getOid() + " created!" );
            endpointUpdated( (JmsEndpoint)createdEntity, true );
        }

        @Override
        public boolean notifyUpdate(long updatedOid, int version) {
            return super.notifyUpdate(updatedOid, version);
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
        protected void onDelete( long removedOid ) {
            logger.info( "Connection " + removedOid + " deleted!" );
            connectionDeleted( removedOid );
        }

        @Override
        protected void onUpdate( PersistentEntity updatedEntity ) {
            logger.info( "Connection " + updatedEntity.getOid() + " updated!" );
            connectionUpdated( (JmsConnection)updatedEntity );
        }

        @Override
        protected void onCreate(PersistentEntity createdEntity) {
            logger.info( "Connection " + createdEntity.getOid() + " created!" );
            connectionUpdated( (JmsConnection)createdEntity );
        }

        @Override
        public boolean notifyUpdate(long updatedOid, int version) {
            return super.notifyUpdate(updatedOid, version);
        }
    }

}