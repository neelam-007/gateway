/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.message.TransportMetadata;
import com.l7tech.message.TransportProtocol;

import javax.jms.Message;
import javax.jms.JMSException;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsTransportMetadata extends TransportMetadata {
    public JmsTransportMetadata( Message request, Message response ) {
        _request = request;
        _response = response;
    }

    public Message getRequest() {
        return _request;
    }

    public Message getResponse() {
        return _response;
    }

    public TransportProtocol getProtocol() {
        return TransportProtocol.JMS;
    }

    public Object getParameter( String name ) {
        try {
            return _request.getObjectProperty( name );
        } catch ( JMSException e ) {
            return null;
        }
    }

    private final Message _request;
    private final Message _response;
}
