package com.l7tech.gateway.common.admin;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 28, 2008
 * Time: 4:25:58 PM
 * 
 */
public class ValidationRuntimeException extends RuntimeAdminException{
    
    public ValidationRuntimeException() {
    }

    public ValidationRuntimeException(String string) {
        super(string);
    }

    public ValidationRuntimeException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public ValidationRuntimeException(Throwable throwable) {
        super(throwable);
    }
}
