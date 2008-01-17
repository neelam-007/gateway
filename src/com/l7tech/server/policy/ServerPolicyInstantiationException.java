package com.l7tech.server.policy;

/**
 * Exception class for errors during construction of server polcies.
 *
 * @author steve
 */
public class ServerPolicyInstantiationException extends Exception {

    public ServerPolicyInstantiationException( Throwable cause ) {
        super( cause );
    }

    public ServerPolicyInstantiationException( String message ) {
        super( message );
    }

    public ServerPolicyInstantiationException( String message, Throwable cause ) {
        super( message, cause );
    }
}
