/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.jms.JmsDestination;
import com.l7tech.jms.JmsProvider;
import com.l7tech.jms.JmsReplyType;
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
            Collection providers = _manager.findAllHeaders();
            for (Iterator i = providers.iterator(); i.hasNext();) {
                JmsProvider provider = (JmsProvider)i.next();
                _logger.info( "Initializing JMS receiver '" + provider.getName() + "'..." );
                Set destinations = provider.getDestinations();
                for (Iterator j = destinations.iterator(); j.hasNext();) {
                    JmsDestination requestDest = (JmsDestination) j.next();
                    JmsDestination replyToDest = requestDest.getReplyDestination();
                    JmsDestination failureDest = requestDest.getFailureDestination();
                    JmsReceiver receiver = new JmsReceiver(
                            provider,
                            JmsReplyType.AUTOMATIC, // TODO Hardcoded
                            requestDest, replyToDest, failureDest );
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
            _logger.info( "Starting JMS receiver '" + receiver.getProvider().getName() + "'" );
            receiver.start();
        }
    }

    public void stop() throws LifecycleException {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            _logger.info( "Stopping JMS receiver '" + receiver.getProvider().getName() + "'" );
            receiver.stop();
        }
    }

    public void close() throws LifecycleException {
        for (Iterator i = _receivers.iterator(); i.hasNext();) {
            JmsReceiver receiver = (JmsReceiver) i.next();
            _logger.info( "Closing JMS receiver '" + receiver.getProvider().getName() + "'" );
            receiver.close();
        }
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
    private JmsManager _manager;
    private boolean _booted = false;
    private Set _receivers = new HashSet();
}
