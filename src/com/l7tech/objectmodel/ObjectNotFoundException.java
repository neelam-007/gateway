/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * Thrown to indicate that an object cannot be found.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ObjectNotFoundException extends ObjectModelException {
    public ObjectNotFoundException() {
        super();
    }

    public ObjectNotFoundException(String message) {
        this(message, null);
    }

    public ObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
