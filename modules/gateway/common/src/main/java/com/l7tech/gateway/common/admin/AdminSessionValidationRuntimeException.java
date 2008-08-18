package com.l7tech.gateway.common.admin;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 28, 2008
 * Time: 4:25:58 PM
 * 
 */
public class AdminSessionValidationRuntimeException extends RuntimeAdminException{
    
    public AdminSessionValidationRuntimeException() {
    }

    public AdminSessionValidationRuntimeException(String string) {
        super(string);
    }

    public AdminSessionValidationRuntimeException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public AdminSessionValidationRuntimeException(Throwable throwable) {
        super(throwable);
    }
}
