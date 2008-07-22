/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.util.ExceptionUtils;

/**
 * @author alex
 * @version $Revision$
 */
public class LifecycleException extends Exception {
    public LifecycleException( String message ) {
        super( message );
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public LifecycleException(Throwable cause) {
        super("Lifecycle error: " + ExceptionUtils.getMessage(cause), cause);
    }
}
