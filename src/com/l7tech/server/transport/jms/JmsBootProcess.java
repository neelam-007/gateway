/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsReplyType;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsBootProcess implements ServerComponentLifecycle {
    public JmsBootProcess() {
        _manager = new JmsManager();
    }

    public void init() throws LifecycleException {
        if ( _booted ) throw new LifecycleException( "Can't boot JmsBootProcess twice!" );

        try {
            Collection connections = _manager.findAll();
            for (Iterator i = connections.iterator(); i.hasNext();) {
                JmsConnection conn = (JmsConnection)i.next();
                _logger.info( "Initializing JMS receiver '" + conn.getName() + "'..." );
                Set endpoints = conn.getEndpoints();
                for (Iterator j = endpoints.iterator(); j.hasNext();) {
                    JmsEndpoint requestEnd = (JmsEndpoint) j.next();
                    JmsEndpoint replyToEnd = requestEnd.getReplyEndpoint();
                    JmsEndpoint failureEnd = requestEnd.getFailureEndpoint();
                    _logger.info( "Initializing JMS receiver for '" + conn.getName() + "/" + requestEnd.getName() + "'" );
                    JmsReceiver receiver = new JmsReceiver(
                            conn,
                            JmsReplyType.AUTOMATIC, // TODO Hardcoded
                            requestEnd, replyToEnd, failureEnd );
                    _receivers.add( receiver );
                }
            }
            _booted = true;
        } catch (FindException e) {
            throw new LifecycleException( e.toString(), e );
        }
    }

    public void start() throws LifecycleException {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            _logger.info( "Starting JMS receiver '" + receiver.toString() + "'..." );
            receiver.start();
        }
    }

    public void stop() throws LifecycleException {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            _logger.info( "Stopping JMS receiver '" + receiver.toString() + "'" );
            receiver.stop();
        }
    }

    public void close() throws LifecycleException {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            _logger.info( "Closing JMS receiver '" + receiver.toString() + "'" );
            receiver.close();
        }
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
    private JmsManager _manager;
    private boolean _booted = false;
    private Set _receivers = new HashSet();
}
