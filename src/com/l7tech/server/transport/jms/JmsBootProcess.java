/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.LicenseManager;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.PeriodicVersionCheck;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.LifecycleBean;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsBootProcess extends LifecycleBean {
    private final ServerConfig serverConfig;
    private final JmsConnectionManager connectionManager;
    private final JmsEndpointManager endpointManager;

    private Object receiverLock = new Object();
    private Set<JmsReceiver> activeReceivers = new HashSet<JmsReceiver>();

    private static final Logger logger = Logger.getLogger(JmsBootProcess.class.getName());
    private final Timer backgroundTimer;
    private boolean started = false;
    private ConnectionVersionChecker connectionChecker;
    private PeriodicVersionCheck endpointChecker;


    public JmsBootProcess(ServerConfig serverConfig,
                          LicenseManager licenseManager,
                          JmsConnectionManager connectionManager,
                          JmsEndpointManager endpointManager,
                          Timer timer)
    {
        super("JMS Boot Process", logger, GatewayFeatureSets.SERVICE_JMS_MESSAGE_INPUT, licenseManager);

        if (timer == null)
            timer = new Timer("JMS config refresh", true);

        this.serverConfig = serverConfig;
        this.connectionManager = connectionManager;
        this.endpointManager = endpointManager;
        this.backgroundTimer = timer;
    }

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

        protected void onDelete( long removedOid ) {
            logger.info( "Endpoint " + removedOid + " deleted!" );
            endpointDeleted( removedOid );
        }

        protected void onSave( PersistentEntity updatedEntity ) {
            logger.info( "Endpoint " + updatedEntity.getOid() + " created or updated!" );
            endpointUpdated( (JmsEndpoint)updatedEntity );
        }
    }

    /**
     * Periodically checks for new, updated or deleted JMS connections
     */
    private class ConnectionVersionChecker extends PeriodicVersionCheck {
        ConnectionVersionChecker( JmsConnectionManager mgr ) throws FindException {
            super(mgr);
        }

        protected void onDelete( long removedOid ) {
            logger.info( "Connection " + removedOid + " deleted!" );
            connectionDeleted( removedOid );
        }

        protected void onSave( PersistentEntity updatedEntity ) {
            logger.info( "Connection " + updatedEntity.getOid() + " created or updated!" );
            connectionUpdated( (JmsConnection)updatedEntity );
        }
    }


    /**
     * Starts {@link JmsReceiver}s for the initial configuration.  Also starts the {@link EndpointVersionChecker} and {@link ConnectionVersionChecker} to periodically
     * check whether endpoints or connections have been created, updated or deleted.
     * <p/>
     * Any exception that is thrown in a JmsReceiver's start() method will be logged but not propagated.
     */
    protected void doStart() throws LifecycleException {
        synchronized(receiverLock) {
            if (started) return;
            started = true;  // "started" just means that we have already once attempted to start the JMS listener subsystem

            logger.info("JMS starting.");

            try {
                // Start up receivers for initial configuration
                Collection<JmsConnection> connections = connectionManager.findAll();
                List<Long> staleEndpoints = new ArrayList<Long>();
                for (JmsConnection connection : connections) {
                    JmsEndpoint[] endpoints = endpointManager.findEndpointsForConnection(connection.getOid());

                    for (JmsEndpoint endpoint : endpoints) {
                        if (!endpoint.isMessageSource()) continue;
                        JmsReceiver receiver = new JmsReceiver(connection, endpoint, endpoint.getReplyType());

                        try {
                            receiver.setApplicationContext(getApplicationContext());
                            receiver.setServerConfig(serverConfig);
                            receiver.start();
                            activeReceivers.add(receiver);
                        } catch (LifecycleException e) {
                            logger.log(Level.WARNING, "Couldn't start receiver for endpoint " + endpoint
                                    + ".  Will retry periodically", e);
                            staleEndpoints.add(new Long(endpoint.getOid()));
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

    /**
     * Attempts to stop all running JMS receivers, then stops the {@link EndpointVersionChecker} and
     * {@link ConnectionVersionChecker}
     */
    protected void doStop() {
        synchronized(receiverLock) {
            if (connectionChecker != null)
                connectionChecker.cancel();
            if (endpointChecker != null)
                endpointChecker.cancel();
            for (JmsReceiver receiver : activeReceivers) {
                logger.info("Stopping and closing JMS receiver '" + receiver.toString() + "'");
                stopAndClose(receiver);
            }
        }
    }

    /**
     * Stops a JmsReceiver. Logs but does not propagate any exception that might be thrown.
     */
    private void stopAndClose(JmsReceiver component) {
        try {
            component.stop();
            component.close();
        } catch (LifecycleException e) {
            logger.warning("Exception while stopping or closing " + component);
        }
    }

    /**
     * Handles the event fired by the deletion of a JmsConnection.
     */
    private void connectionDeleted( long deletedConnectionOid ) {
        synchronized(receiverLock) {
            for (Iterator i = activeReceivers.iterator(); i.hasNext();) {
                JmsReceiver receiver = (JmsReceiver)i.next();
                if (receiver.getConnection().getOid() == deletedConnectionOid ) {
                    stopAndClose(receiver);
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
                // Stop and remove any existing receivers for this connection
                connectionDeleted( updatedOid );

                EntityHeader[] endpoints = endpointManager.findEndpointHeadersForConnection( updatedOid );
                for (EntityHeader header : endpoints) {
                    JmsEndpoint endpoint = endpointManager.findByPrimaryKey(header.getOid());
                    endpointUpdated(endpoint);
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
                    stopAndClose(receiver);
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
    private void endpointUpdated( JmsEndpoint updatedEndpoint ) {
        // Stop any existing receivers for this endpoint
        endpointDeleted( updatedEndpoint.getOid() );

        if (updatedEndpoint.isMessageSource()) {
            JmsReceiver receiver = null;
            try {
                JmsConnection connection = connectionManager.findByPrimaryKey( updatedEndpoint.getConnectionOid() );
                receiver = new JmsReceiver(connection, updatedEndpoint, updatedEndpoint.getReplyType());
                receiver.setApplicationContext(getApplicationContext());
                receiver.setServerConfig(serverConfig);
                receiver.start();
                synchronized(receiverLock) {
                    activeReceivers.add(receiver);
                }
            } catch (LifecycleException e) {
                logger.warning("Exception while initializing receiver " + receiver +
                                "; will try again later: " + e.toString());
                try {
                    Thread.sleep(500);
                } catch ( InterruptedException e1 ) {
                    Thread.currentThread().interrupt();
                }
                synchronized(receiverLock) {
                    if (endpointChecker != null)
                        endpointChecker.markObjectAsStale( new Long(updatedEndpoint.getOid()));
                }
            } catch ( FindException e ) {
                logger.log( Level.SEVERE, "Couldn't find connection for endpoint " + updatedEndpoint, e );
            }
        }
    }

}
