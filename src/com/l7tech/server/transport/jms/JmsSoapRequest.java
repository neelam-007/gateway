/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.util.CausedIOException;
import com.l7tech.logging.LogManager;
import com.l7tech.message.SoapRequest;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsSoapRequest extends SoapRequest {
    private Logger _logger = LogManager.getInstance().getSystemLogger();

    public JmsSoapRequest( JmsTransportMetadata mt ) {
        super( mt );
        _jms = mt;
    }

    protected Reader doGetRequestReader() throws IOException {
        Message request = _jms.getRequest();
        if ( request instanceof TextMessage ) {
            TextMessage treq = (TextMessage)request;
            try {
                return new StringReader( treq.getText() );
            } catch (JMSException e) {
                throw new IOException( e.toString() );
            }
        } else if ( request instanceof BytesMessage ) {
             // TODO read XML header and guess encoding

            final BytesMessage breq = (BytesMessage)request;

            InputStream is = new BytesMessageInputStream( breq );
            return new InputStreamReader( is, JmsUtil.DEFAULT_ENCODING );     // todo sane encoding
        } else {
            _logger.warning( "Can't get a reader for a non-text message! Returning a reader on an empty String!" );
            return new StringReader("");
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
