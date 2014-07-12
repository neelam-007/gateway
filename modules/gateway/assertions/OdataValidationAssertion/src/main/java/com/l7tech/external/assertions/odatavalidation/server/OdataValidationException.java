package com.l7tech.external.assertions.odatavalidation.server;

/**
 * Created by yuri on 04/07/14.
 */
public class OdataValidationException extends Exception{

    public OdataValidationException(String message) {
        super(message);
    }

    public OdataValidationException(String message, Throwable err) {
        super(message, err);
    }
}
