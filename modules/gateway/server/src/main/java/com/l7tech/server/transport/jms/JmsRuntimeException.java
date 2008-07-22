/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsRuntimeException extends Exception {
    public JmsRuntimeException( String message ) {
        super( message );
    }

    public JmsRuntimeException(Throwable e) {
        super(e);
    }

    public JmsRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
