package com.l7tech.common.security.xml;

/**
 * A signature is invalid or of type unsupported.
 *
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Aug 20, 2003<br/>
 * $Id$
 *
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
