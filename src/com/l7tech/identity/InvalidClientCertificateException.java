/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

/**
 * @author alex
 * @version $Revision$
 */
public class InvalidClientCertificateException extends AuthenticationException {
    public InvalidClientCertificateException() {
        super();
    }

    public InvalidClientCertificateException( String message ) {
        super( message );
    }

    public InvalidClientCertificateException( String message, Throwable cause ) {
        super( message, cause );
    }
}
