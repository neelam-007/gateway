package com.l7tech.common.security.xml;

/**
 * Expresses the fact that no digital signature were found.
 *
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Aug 20, 2003<br/>
 * $Id$
 */
public class SignatureNotFoundException extends Exception {
    public SignatureNotFoundException(String msg) {
        super(msg);
    }

    public SignatureNotFoundException(Throwable e) {
        super(e.getMessage(), e);
    }

    public SignatureNotFoundException(String msg, Throwable e) {
        super(msg, e);
    }
}
