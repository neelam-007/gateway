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
    JmsRuntimeException( String message ) {
        super( message );
    }

    JmsRuntimeException(Throwable e) {
        super(e);
    }
}
