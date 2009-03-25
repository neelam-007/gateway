package com.l7tech.server.ems.gateway;

import javax.xml.ws.WebServiceException;

/**
 * Exception thrown if a Gateway node or cluster cannot be accessed due to a network (or SOAP) problem.

 * @author jbufu
 */
public class FailoverException extends WebServiceException {

    public FailoverException() {
    }

    public FailoverException(String message) {
        super(message);
    }

    public FailoverException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailoverException(Throwable cause) {
        super(cause);
    }

}
