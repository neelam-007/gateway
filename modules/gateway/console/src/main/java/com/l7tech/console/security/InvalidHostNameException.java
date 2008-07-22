package com.l7tech.console.security;

import javax.security.auth.login.LoginException;

/**
 * Exception thrown when there is a hostname problem.
 *
 * @author $Author$
 * @version $Revision$
 */
public class InvalidHostNameException extends SecurityException {

    //- PUBLIC

    /**
     * Create an invalid host name exception.
     *
     * @param expectedHost the "network" name used to connect.
     * @param actualHost the name that was encountered
     */
    public InvalidHostNameException(String expectedHost, String actualHost) {
        super("Expected host name '"+expectedHost+"', but name was '"+actualHost+"'.");
        this.expectedHost = expectedHost;
        this.actualHost = actualHost;
    }

    /**
     * Get the expected host name.
     *
     * @return the host name
     */
    public String getExpectedHost() {
        return expectedHost;
    }

    /**
     * Get the actual host name.
     *
     * @return the host name
     */
    public String getActualHost() {
        return actualHost;
    }

    //- PRIVATE

    private final String expectedHost;
    private final String actualHost;

}
