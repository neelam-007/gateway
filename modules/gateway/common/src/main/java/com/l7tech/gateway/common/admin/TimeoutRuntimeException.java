package com.l7tech.gateway.common.admin;

/**
 * The exception handles timeout exceptions when securespan manager
 * tries to connect mysql or gateway.
 *  
 * @author: ghuang
 */
public class TimeoutRuntimeException extends RuntimeAdminException {

    public TimeoutRuntimeException() {
    }

    public TimeoutRuntimeException(String string) {
        super(string);
    }

    public TimeoutRuntimeException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public TimeoutRuntimeException(Throwable throwable) {
        super(throwable);
    }
}
