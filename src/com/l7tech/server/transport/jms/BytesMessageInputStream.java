/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.util.CausedIOException;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import java.io.InputStream;
import java.io.IOException;

/**
 * Adaptor that provides an InputStream view of the contents of a JMS BytesMessage.
 * @author alex
 * @version $Revision$
 */
public class BytesMessageInputStream extends InputStream {
    public BytesMessageInputStream( BytesMessage breq ) {
        this.breq = breq;
    }

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

    private final BytesMessage breq;
}
