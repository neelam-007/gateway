/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

/**
 * @author alex
 * @version $Revision$
 */
public class LifecycleException extends Exception {
    public LifecycleException( String message ) {
        super( message );
    }

    public LifecycleException( String message, Throwable cause ) {
        super( message, cause );
    }
}
