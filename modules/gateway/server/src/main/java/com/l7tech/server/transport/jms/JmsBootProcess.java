/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.*;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.FaultProcessed;
import org.springframework.context.ApplicationEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsBootProcess extends LifecycleBean implements PropertyChangeListener {
    private final ServerConfig serverConfig;
    private final JmsConnectionManager connectionManager;
    private final JmsEndpointManager endpointManager;
    private final JmsPropertyMapper jmsPropertyMapper;

    private final Object receiverLock = new Object();
    private Set<JmsReceiver> activeReceivers = new HashSet<JmsReceiver>();

    private static final Logger logger = Logger.getLogger(JmsBootProcess.class.getName());
    private final Timer backgroundTimer;
    private boolean started = false;

    // it is expected that these checkers are run on the same thread
    private ConnectionVersionChecker connectionChecker;
    private EndpointVersionChecker endpointChecker;

    public JmsBootProcess(ServerConfig serverConfig,
                          LicenseManager licenseManager,
                          JmsConnectionManager connectionManager,
                          JmsEndpointManager endpointManager,
                          JmsPropertyMapper jmsPropertyMapper,
                          Timer timer)
    {
        super("JMS Boot Process", logger, GatewayFeatureSets.SERVICE_JMS_MESSAGE_INPUT, licenseManager);

        if (timer == null)
            timer = new Timer("JMS config refresh", true);

        this.serverConfig = serverConfig;
        this.connectionManager = connectionManager;
        this.endpointManager = endpointManager;
        this.jmsPropertyMapper = jmsPropertyMapper;
        this.backgroundTimer = timer;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        synchronized(receiverLock) {
            for (JmsReceiver receiver : activeReceivers) {
                receiver.propertyChange(evt);
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


    /**
     * Starts {@link JmsReceiver}s for the initial configuration.  Also starts the EndpointVersionChecker and ConnectionVersionChecker to periodically
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

    private void startReceivers() throws LifecycleException {
        synchronized(receiverLock) {
            if (started) return;

            try {
                started = true;  // "started" just means that we have already once attempted to start the JMS listener subsystem
                logger.info("JMS starting.");
                // Start up receivers for initial configuration
                Collection<JmsConnection> connections = connectionManager.findAll();
                List<Long> staleEndpoints = new ArrayList<Long>();
                for (JmsConnection connection : connections) {
                    JmsEndpoint[] endpoints = endpointManager.findEndpointsForConnection(connection.getOid());

                    for (JmsEndpoint endpoint : endpoints) {
                        if (!endpoint.isMessageSource() || endpoint.isDisabled()) continue;
                        JmsReceiver receiver = new JmsReceiver(connection, endpoint, endpoint.getReplyType(), jmsPropertyMapper, getApplicationContext());

                        try {
                            receiver.start();
                            activeReceivers.add(receiver);
                        } catch (LifecycleException e) {
                            logger.log(Level.WARNING, "Couldn't start receiver for endpoint " + endpoint
                                    + ".  Will retry periodically", e);
                            staleEndpoints.add(endpoint.getOid());
                        }
                    }
                }

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
                startReceivers();
            } catch (LifecycleException e) {
                logger.log(Level.SEVERE, "Unable to start JMS Receivers", e);
            }
        }
    }

    /**
     * Attempts to stop all running JMS receivers, then stops the EndpointVersionChecker and
     * ConnectionVersionChecker
     */
    @Override
    protected void doStop() {
        synchronized(receiverLock) {
            if (connectionChecker != null)
                connectionChecker.cancel();
            if (endpointChecker != null)
                endpointChecker.cancel();
            for (JmsReceiver receiver : activeReceivers) {
                logger.info("Stopping JMS receiver '" + receiver.toString() + "'");
                receiver.stop();
            }
            for (JmsReceiver receiver : activeReceivers) {
                logger.info("Waiting for JMS receiver to stop '" + receiver.toString() + "'");
                receiver.ensureStopped();
            }
            activeReceivers.clear();
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
        synchronized(receiverLock) {
            for (Iterator i = activeReceivers.iterator(); i.hasNext();) {
                JmsReceiver receiver = (JmsReceiver)i.next();
                if (receiver.getConnection().getOid() == deletedConnectionOid ) {
                    receiver.stop();
                    i.remove();
                }
            }
        }
    }

    /**
     * Handles the event fired by the update of a JmsConnection.
     */
    private void connectionUpdated(JmsConnection updatedConnection) {
        synchronized(receiverLock) {
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
        synchronized(receiverLock) {
            for (Iterator i = activeReceivers.iterator(); i.hasNext();) {
                JmsReceiver receiver = (JmsReceiver)i.next();
                JmsEndpoint existingEndpoint = receiver.getInboundRequestEndpoint();
                if (existingEndpoint.getOid() == deletedEndpointOid ) {
                    receiver.stop();
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
        endpointDeleted( updatedEndpoint.getOid() );

        if (updatedEndpoint.isMessageSource()) {
            if (updatedEndpoint.isDisabled()) return;
            JmsReceiver receiver = null;
            try {
                JmsConnection connection = connectionManager.findByPrimaryKey( updatedEndpoint.getConnectionOid() );
                if (notify)
                    connectionChecker.notifyUpdate(connection.getOid(), connection.getVersion());

                receiver = new JmsReceiver(connection, updatedEndpoint, updatedEndpoint.getReplyType(), jmsPropertyMapper, getApplicationContext());
                receiver.start();
                synchronized(receiverLock) {
                    activeReceivers.add(receiver);
                }
            } catch (LifecycleException e) {
                logger.warning("Exception while initializing receiver " + receiver +
                                "; will try again later: " + e.toString());
                synchronized(receiverLock) {
                    if (endpointChecker != null)
                        endpointChecker.markObjectAsStale( updatedEndpoint.getOid() );
                }
            } catch ( FindException e ) {
                logger.log( Level.SEVERE, "Couldn't find connection for endpoint " + updatedEndpoint, e );
            }
        }
    }
}
