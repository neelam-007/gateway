package com.l7tech.common.security.xml;

/**
 * User: flascell
 * Date: Aug 20, 2003
 * Time: 10:33:00 AM
 * $Id$
 *
 * Express the fact that no digital signature were found.
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
