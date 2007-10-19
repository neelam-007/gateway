package com.l7tech.common.uddi;

/**
 * General purpose base class for UDDI Exceptions.
 *
 * @author Steve Jones
*/
public class UDDIException extends Exception {

    //- PUBLIC

    public UDDIException(Throwable cause) {
        super(cause);
    }

    public UDDIException(String message) {
        super(message);
    }

    public UDDIException(String message, Throwable cause) {
        super(message, cause);
    }

    //- PROTECTED

    protected UDDIException() {
        super();
    }
    
}
