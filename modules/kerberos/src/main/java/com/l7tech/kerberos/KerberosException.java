package com.l7tech.kerberos;

/**
 * Exception thrown on Kerberos errors.
 *
 * @author $Author$
 * @version $Revision$
 */
public class KerberosException extends Exception {
    public KerberosException() {
        super();
    }

    public KerberosException(String message) {
        super(message);
    }

    public KerberosException(String message, Throwable cause) {
        super(message, cause);
    }

    public KerberosException(Throwable cause) {
        super(cause);
    }
}
