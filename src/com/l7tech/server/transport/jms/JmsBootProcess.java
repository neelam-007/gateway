/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;
import com.l7tech.server.ServerConfig;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsBootProcess implements ServerComponentLifecycle, JmsCrudListener {
    public JmsBootProcess() {
        _manager = new JmsManager();
    }

    public String toString() {
        return "JMS Boot Process";
    }

    public void init( ServerConfig config ) throws LifecycleException {
        if ( _booted ) throw new LifecycleException( "Can't boot JmsBootProcess twice!" );

        try {
            Collection endpoints = _manager.findMessageSourceEndpoints();
            for ( Iterator i = endpoints.iterator(); i.hasNext(); ) {
                JmsEndpoint requestEnd = (JmsEndpoint) i.next();
                JmsConnection conn = requestEnd.getConnection();

                _logger.info( "Initializing JMS receiver for '" + conn.getName() + "/" + requestEnd.getName() + "'" );
                JmsReceiver receiver = makeReceiver( requestEnd );

                init( receiver );
                _receivers.add( receiver );
            }

            _manager.addCrudListener( this );

            _booted = true;
        } catch (FindException e) {
            throw new LifecycleException( e.toString(), e );
        }
    }

    /**
     * Attempts to start all JMS receivers.
     * <p/>
     * Any exception that is thrown in a JmsReceiver's start() method will be logged but not propagated.
     */
    public void start() {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            _logger.info( "Starting JMS receiver '" + receiver.toString() + "'..." );
            start( receiver );
        }
    }

    /**
     * Attempts to stop all JMS receivers.
     */
    public void stop() {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            _logger.info( "Stopping JMS receiver '" + receiver.toString() + "'" );
            stop( receiver );
        }
    }

    /**
     * Attempts to close all JMS receivers.
     */
    public void close() {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            _logger.info( "Closing JMS receiver '" + receiver.toString() + "'" );
            close(receiver);
        }
    }

    /**
     * Handles the event fired by the deletion of a JmsConnection.
     */
    public void connectionDeleted( JmsConnection connection ) {
        for ( Iterator i = _receivers.iterator(); i.hasNext(); ) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            if ( receiver.getConnection().getOid() == connection.getOid() ) {
                stop( receiver );
                close( receiver );
            }
        }
    }

    /**
     * Stops a JmsReceiver. Logs but does not propagate any exception that might be thrown.
     */
    private void stop( JmsReceiver component ) {
        try {
            component.stop();
        } catch ( LifecycleException e ) {
            _logger.warning( "Exception while stopping " + component );
        }
    }

    /**
     * Closes a JmsReceiver.  Logs but does not propagate any exception that might be thrown.
     */
    private void close( JmsReceiver component ) {
        try {
            component.close();
        } catch ( LifecycleException e ) {
            _logger.warning( "Exception while closing " + component );
        }
    }

    /**
     * Initializes a JmsReceiver. Logs but does not propagate any exception that might be thrown.
     */
    private void init( JmsReceiver component ) {
        try {
            component.init( ServerConfig.getInstance() );
        } catch ( LifecycleException e ) {
            _logger.warning( "Exception while initializing " + component );
        }
    }

    /**
     * Starts a JmsReceiver. Logs but does not propagate any exception that might be thrown.
     */
    private void start( JmsReceiver component ) {
        try {
            component.start();
        } catch ( LifecycleException e ) {
            _logger.warning( "Exception while initializing " + component );
        }
    }

    /**
     * Handles the event fired by the update of a JmsConnection.
     */
    public synchronized void connectionUpdated( JmsConnection updatedConnection ) {
        for ( Iterator i = _receivers.iterator(); i.hasNext(); ) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            stop(receiver);
            close(receiver);
            _receivers.remove(receiver);
        }

        for ( Iterator i = updatedConnection.getEndpoints().iterator(); i.hasNext(); ) {
            JmsEndpoint endpoint = (JmsEndpoint)i.next();
            JmsReceiver receiver = makeReceiver( endpoint );
            try {
                receiver.init( ServerConfig.getInstance() );
                receiver.start();
                _receivers.add( receiver );
            } catch ( LifecycleException e ) {
                _logger.warning( "Exception while initializing " + receiver );
            }
        }
    }

    /**
     * Handles the event fired by the deletion of a JmsEndpoint.
     */
    public synchronized void endpointDeleted( JmsEndpoint deletedEndpoint ) {
        for ( Iterator i = _receivers.iterator(); i.hasNext(); ) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            JmsEndpoint existingEndpoint = receiver.getInboundRequestEndpoint();
            if ( existingEndpoint.getOid() == deletedEndpoint.getOid() ) {
                stop(receiver);
                close(receiver);
            }
        }
    }

    /**
     * Handles the event fired by the update of a JmsEndpoint.
     */
    public synchronized void endpointUpdated( JmsEndpoint updatedEndpoint ) {
        for ( Iterator i = _receivers.iterator(); i.hasNext(); ) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            JmsEndpoint originalEndpoint = receiver.getInboundRequestEndpoint();
            if ( originalEndpoint.getOid() == updatedEndpoint.getOid() ) {
                stop(receiver);
                close(receiver);
                _receivers.remove(receiver);
            }
        }

        if ( updatedEndpoint.isMessageSource() ) {
            JmsReceiver receiver = makeReceiver( updatedEndpoint );
            try {
                receiver.init( ServerConfig.getInstance() );
                receiver.start();
                _receivers.add( receiver );
            } catch ( LifecycleException e ) {
                _logger.warning( "Exception while initializing " + receiver );
            }
        }
    }

    private JmsReceiver makeReceiver( JmsEndpoint endpoint ) {
        JmsReceiver receiver = new JmsReceiver( endpoint,
                                                endpoint.getReplyType(),
                                                endpoint.getReplyEndpoint(),
                                                endpoint.getFailureEndpoint() );
        return receiver;
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
    private JmsManager _manager;
    private boolean _booted = false;
    private Set _receivers = new HashSet();
}
