/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessingException extends Exception {
    public MessageProcessingException() {
        super();
    }

    public MessageProcessingException( String message ) {
        super( message );
    }

    public MessageProcessingException( String message, Throwable cause ) {
        super( message, cause );
    }
}
