/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.message.SoapRequest;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsSoapRequest extends SoapRequest {
    private final Logger _logger = Logger.getLogger(getClass().getName());

    public JmsSoapRequest( JmsTransportMetadata mt ) {
        super( mt );
        _jms = mt;
    }

    protected InputStream doGetRequestInputStream() throws IOException {
        Message request = _jms.getRequest();
        if ( request instanceof TextMessage ) {
            TextMessage treq = (TextMessage)request;
            try {
                return new ByteArrayInputStream( treq.getText().getBytes() );
            } catch (JMSException e) {
                throw new IOException( e.toString() );
            }
        } else if ( request instanceof BytesMessage ) {
             // TODO read XML header and guess encoding

            final BytesMessage breq = (BytesMessage)request;

            return new BytesMessageInputStream( breq );
        } else {
            _logger.warning( "Can't get a reader for a non-text message! Returning a reader on an empty String!" );
            return new ByteArrayInputStream( new String("").getBytes() );
        }
    }

    public boolean isReplyExpected() {
        Message msg = _jms.getRequest();
        try {
            return msg.getJMSReplyTo() != null || 
                   msg.getJMSCorrelationID() != null ||
                   msg.getJMSCorrelationIDAsBytes().length > 0;
        } catch ( JMSException e ) {
            _logger.log( Level.SEVERE, "Caught JMSException", e );
        }
        return false;
    }

    private JmsTransportMetadata _jms;
}
