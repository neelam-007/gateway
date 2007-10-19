package com.l7tech.common.uddi;

/**
 * UDDI Exception thrown on invalid data.
 *
 * @author Steve Jones
 */
public class UDDIDataException extends UDDIException {

    public UDDIDataException(String message) {
        super(message);
    }

    public UDDIDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
