/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.processor;

/**
 * Exception thrown during policy processing if the SSG username and/or password is found to be wrong.
 * User: mike
 * Date: Aug 22, 2003
 * Time: 3:26:17 PM
 */
public class BadCredentialsException extends MessageProcessorException {
    public BadCredentialsException() {
    }

    public BadCredentialsException(String message) {
        super(message);
    }

    public BadCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadCredentialsException(Throwable cause) {
        super(cause);
    }
}
