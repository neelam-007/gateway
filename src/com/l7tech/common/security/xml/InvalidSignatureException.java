package com.l7tech.common.security.xml;

/**
 * User: flascell
 * Date: Aug 20, 2003
 * Time: 10:52:49 AM
 * $Id$
 *
 * A signature is invalid or of type unsupported
 */
public class InvalidSignatureException extends Exception {
    public InvalidSignatureException(String msg) {
        super(msg);
    }

    public InvalidSignatureException(Throwable e) {
        super(e.getMessage(), e);
    }

    public InvalidSignatureException(String msg, Throwable e) {
        super(msg, e);
    }
}
