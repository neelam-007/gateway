/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.message.SoapRequest;
import com.l7tech.logging.LogManager;

import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.JMSException;
import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;

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
        } else {
            _logger.warning( "Can't get a reader for a non-text message! Returning a reader on an empty String!" );
            return new StringReader("");
        }
    }

    private JmsTransportMetadata _jms;
}
