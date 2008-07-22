/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.wstrust;

/**
 * @author mike
 */
public class WsTrustConfigException extends Exception {
    public WsTrustConfigException() {
    }

    public WsTrustConfigException(String message) {
        super(message);
    }

    public WsTrustConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public WsTrustConfigException(Throwable cause) {
        super(cause);
    }
}
