/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.SoapRequest;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsSoapRequest extends SoapRequest {
    public Object doGetParameter( String name ) {
        return _transportMetadata.getRequestParameter(name);
    }

    private final Logger _logger = Logger.getLogger(getClass().getName());

    public JmsSoapRequest( JmsTransportMetadata mt ) throws IOException {
        super( mt );
        _jms = mt;
        // Initialize myself
        initialize(doGetRequestInputStream(), ContentTypeHeader.XML_DEFAULT);
    }

    protected InputStream doGetRequestInputStream() throws IOException {
        Message request = _jms.getRequest();
        if ( request instanceof TextMessage ) {
            TextMessage treq = (TextMessage)request;
            try {
                return new ByteArrayInputStream( treq.getText().getBytes(JmsUtil.DEFAULT_ENCODING) );
            } catch (JMSException e) {
                throw new IOException( e.toString() );
            }
        } else if ( request instanceof BytesMessage ) {
            return new BytesMessageInputStream( (BytesMessage)request );
        } else {
            _logger.warning( "Can't get an InputStream for messages that are neither Text nore Bytes! Returning an empty InputStream!" );
            return new EmptyInputStream();
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
