/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * @author alex
 * @version $Revision$
 */
public class TransactionException extends ObjectModelException {
    public TransactionException() {
        super();
    }

    public TransactionException( String message ) {
        super( message );
    }

    public TransactionException( String message, Throwable cause ) {
        super( message, cause );
    }

    public TransactionException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
