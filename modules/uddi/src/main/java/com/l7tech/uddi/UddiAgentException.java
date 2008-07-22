package com.l7tech.uddi;

/**
 * Exception class for UDDI agents.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class UddiAgentException extends Exception {
    public UddiAgentException() {
        super();
    }

    public UddiAgentException(Throwable cause) {
        super(cause);
    }

    public UddiAgentException(String message) {
        super(message);
    }

    public UddiAgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
