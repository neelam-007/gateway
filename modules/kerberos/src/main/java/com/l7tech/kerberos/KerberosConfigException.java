package com.l7tech.kerberos;

/**
 * Exception thrown on Kerberos configuration errors.
 *
 * @author $Author$
 * @version $Revision$
 */
public class KerberosConfigException extends KerberosException {

    public KerberosConfigException() {
        super();
    }

    public KerberosConfigException(Throwable cause) {
        super(cause);
    }

    public KerberosConfigException(String message) {
        super(message);
    }

    public KerberosConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
