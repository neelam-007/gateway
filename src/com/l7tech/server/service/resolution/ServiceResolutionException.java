/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

/**
 * @author alex
 * @version $Revision$
 */
public class ServiceResolutionException extends Exception {
    public ServiceResolutionException() {
        super();
    }

    public ServiceResolutionException( String message ) {
        super( message );
    }

    public ServiceResolutionException( String message, Throwable cause ) {
        super( message, cause );
    }
}
