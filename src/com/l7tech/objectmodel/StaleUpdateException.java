/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * Indicates an attempt to update a persistent object with data that is obsolete, because
 * the persistent data has been changed since the candidate object was loaded.
 *
 * @author alex
 * @version $Revision$
 */
public class StaleUpdateException extends UpdateException {
    public StaleUpdateException() {
        super();
    }

    public StaleUpdateException( String message ) {
        super( message );
    }

    public StaleUpdateException( String message, Throwable cause ) {
        super( message, cause );
    }
}
