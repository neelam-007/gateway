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

            InputStream is = new InputStream() {
                public int read() throws IOException {
                    try {
                        return breq.readByte();
                    } catch ( final JMSException e ) {
                        throw new CausedIOException( e );
                    }
                }

                public synchronized int read( byte[] out, int off, int len ) throws IOException {
                    try {
                        if ( off == 0 ) {
                            return breq.readBytes( out, len );
                        } else {
                            byte[] temp = new byte[ len ];
                            int num = breq.readBytes( temp, len );
                            if ( num < 0 ) return num;
                            System.arraycopy( temp, 0, out, off, len );
                            return num;
                        }
                    } catch ( final JMSException e ) {
                        throw new CausedIOException( e );
                    }
                }
            };
            return new InputStreamReader( is, DEFAULT_ENCODING );
        } else {
            _logger.warning( "Can't get a reader for a non-text message! Returning a reader on an empty String!" );
            return new StringReader("");
        }
    }

    private JmsTransportMetadata _jms;
    public static final int BUFLEN = 4096;
    public static final String DEFAULT_ENCODING = "UTF-8";
}
