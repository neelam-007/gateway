/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.server.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsBootProcess implements ServerComponentLifecycle {
    public JmsBootProcess() {
        _connectionManager = (JmsConnectionManager)Locator.getDefault().lookup( JmsConnectionManager.class );
        _endpointManager = (JmsEndpointManager)Locator.getDefault().lookup( JmsEndpointManager.class );
        if ( _connectionManager == null || _endpointManager == null ) {
            _logger.severe( "Couldn't find JMS Managers! JMS functionality will be disabled!" );
            _valid = false;
        } else {
            _valid = true;
        }
    }

    public String toString() {
        return "JMS Boot Process";
    }

    public synchronized void init(ComponentConfig config) throws LifecycleException {
        if ( !_valid ) return;
        if (_booted) throw new LifecycleException("Can't boot JmsBootProcess twice!");
        _booted = true;
    }

    /**
     * Periodically checks for new, updated or deleted JMS endpoints
     */
    private class EndpointVersionChecker extends PeriodicVersionCheck {
        EndpointVersionChecker( JmsEndpointManager mgr ) {
            super( mgr );
        }

        protected void onDelete( long removedOid ) {
            _logger.info( "Endpoint " + removedOid + " deleted!" );
            endpointDeleted( removedOid );
        }

        protected void onSave( Entity updatedEntity ) {
            _logger.info( "Endpoint " + updatedEntity.getOid() + " created or updated!" );
            endpointUpdated( (JmsEndpoint)updatedEntity );
        }
    }

    /**
     * Periodically checks for new, updated or deleted JMS connections
     */
    private class ConnectionVersionChecker extends PeriodicVersionCheck {
        ConnectionVersionChecker( JmsConnectionManager mgr ) {
            super(mgr);
        }

        protected void onDelete( long removedOid ) {
            _logger.info( "Connection " + removedOid + " deleted!" );
            connectionDeleted( removedOid );
        }

        protected void onSave( Entity updatedEntity ) {
            _logger.info( "Connection " + updatedEntity.getOid() + " created or updated!" );
            connectionUpdated( (JmsConnection)updatedEntity );
        }
    }


    /**
     * Starts the {@link EndpointVersionChecker} and {@link ConnectionVersionChecker} to periodically
     * check whether endpoints or connections have been created, updated or deleted.
     * <p/>
     * Any exception that is thrown in a JmsReceiver's start() method will be logged but not propagated.
     */
    public synchronized void start() {
        if ( !_valid ) return;

        ConnectionVersionChecker connectionChecker = new ConnectionVersionChecker( _connectionManager );
        EndpointVersionChecker endpointChecker = new EndpointVersionChecker( _endpointManager );

        _connectionVersionTimer.schedule( connectionChecker, connectionChecker.getFrequency() * 2,
                                          connectionChecker.getFrequency() );

        _endpointVersionTimer.schedule( endpointChecker, endpointChecker.getFrequency() * 2,
                                        endpointChecker.getFrequency() );
    }

    /**
     * Attempts to stop all running JMS receivers, then stops the {@link EndpointVersionChecker} and
     * {@link ConnectionVersionChecker}
     */
    public synchronized void stop() {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver)i.next();
            _logger.info("Stopping JMS receiver '" + receiver.toString() + "'");
            stop(receiver);
        }

        _connectionVersionTimer.cancel();
        _endpointVersionTimer.cancel();
    }

    /**
     * Attempts to close all JMS receivers.
     */
    public synchronized void close() {
        _connectionVersionTimer.cancel();
        _endpointVersionTimer.cancel();

        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver)i.next();
            _logger.info("Closing JMS receiver '" + receiver.toString() + "'");
            close(receiver);
        }
    }

    /**
     * Stops a JmsReceiver. Logs but does not propagate any exception that might be thrown.
     */
    private void stop(JmsReceiver component) {
        try {
            component.stop();
        } catch (LifecycleException e) {
            _logger.warning("Exception while stopping " + component);
        }
    }

    /**
     * Closes a JmsReceiver.  Logs but does not propagate any exception that might be thrown.
     */
    private void close(JmsReceiver component) {
        try {
            component.close();
        } catch (LifecycleException e) {
            _logger.warning("Exception while closing " + component);
        }
    }

    /**
     * Handles the event fired by the deletion of a JmsConnection.
     */
    private synchronized void connectionDeleted( long deletedConnectionOid ) {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver)i.next();
            if (receiver.getConnection().getOid() == deletedConnectionOid ) {
                stop(receiver);
                close(receiver);
                i.remove();
            }
        }
    }

    /**
     * Handles the event fired by the update of a JmsConnection.
     */
    private synchronized void connectionUpdated(JmsConnection updatedConnection) {
        long updatedOid = updatedConnection.getOid();

        // Stop and remove any existing receivers for this connection
        connectionDeleted( updatedOid );

        try {
            EntityHeader[] endpoints = _endpointManager.findEndpointHeadersForConnection( updatedOid );
            for ( int i = 0; i < endpoints.length; i++ ) {
                EntityHeader header = endpoints[i];
                JmsEndpoint endpoint = _endpointManager.findByPrimaryKey( header.getOid() );
                endpointUpdated( endpoint );
            }
        } catch ( FindException e ) {
            _logger.log( Level.SEVERE, "Caught exception finding endpoints for a connection!", e );
        }
    }

    /**
     * Handles the event generated by the discovery of the deletion of a JmsEndpoint.
     *
     * @param deletedEndpointOid the OID of the endpoint that has been deleted.
     */
    private synchronized void endpointDeleted( long deletedEndpointOid ) {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver)i.next();
            JmsEndpoint existingEndpoint = receiver.getInboundRequestEndpoint();
            if (existingEndpoint.getOid() == deletedEndpointOid ) {
                stop(receiver);
                close(receiver);
                i.remove();
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
    private synchronized void endpointUpdated( JmsEndpoint updatedEndpoint ) {
        // Stop any existing receivers for this endpoint
        endpointDeleted( updatedEndpoint.getOid() );

        if (updatedEndpoint.isMessageSource()) {
            JmsReceiver receiver = null;
            try {
                JmsConnection connection = _connectionManager.findConnectionByPrimaryKey( updatedEndpoint.getConnectionOid() );
                receiver = makeReceiver( connection, updatedEndpoint);
                receiver.init(ServerConfig.getInstance());
                receiver.start();
                _receivers.add(receiver);
            } catch (LifecycleException e) {
                _logger.warning("Exception while initializing receiver " + receiver);
            } catch ( FindException e ) {
                _logger.log( Level.SEVERE, "Couldn't find connection for endpoint " + updatedEndpoint, e );
            }
        }
    }

    private JmsReceiver makeReceiver( JmsConnection connection, JmsEndpoint endpoint) {
        JmsReceiver receiver = new JmsReceiver(
                connection,
                endpoint,
                endpoint.getReplyType(),
                endpoint.getReplyEndpoint(),
                endpoint.getFailureEndpoint());
        return receiver;
    }

    private JmsConnectionManager _connectionManager;
    private JmsEndpointManager _endpointManager;
    private Set _receivers = new HashSet();

    private final Timer _connectionVersionTimer = new Timer(true);
    private final Timer _endpointVersionTimer = new Timer(true);

    private Logger _logger = LogManager.getInstance().getSystemLogger();
    private boolean _booted = false;
    private boolean _valid = false;
    public static final int FREQUENCY = 4 * 1000;
}
