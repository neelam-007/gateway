/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

/**
 * Exception used to report SSL problems from way down in the bowels of the engine.
 * Since the keymanager and trustmanager might need to call this, it's a subclass of
 * RuntimeException instead of GeneralSecurityException.
 * User: mike
 * Date: Jul 31, 2003
 * Time: 9:21:50 PM
 */
public class ClientProxySslException extends RuntimeException {
    public ClientProxySslException() {
    }

    public ClientProxySslException(String message) {
        super(message);
    }

    public ClientProxySslException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientProxySslException(Throwable cause) {
        super(cause);
    }
}
