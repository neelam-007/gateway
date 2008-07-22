package com.l7tech.identity;

/**
 * This exception signifies the concept that although some valid non-cert based credentials
 * were authenticated, the identity possesses a valid cert that was not presented over ssl.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 12, 2004<br/>
 * $Id$<br/>
 */
public class IssuedCertNotPresentedException extends AuthenticationException {
    public IssuedCertNotPresentedException() {
        super();
    }

    public IssuedCertNotPresentedException( String message ) {
        super( message );
    }

    public IssuedCertNotPresentedException( String message, Throwable cause ) {
        super( message, cause );
    }
}
