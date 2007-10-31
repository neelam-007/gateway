package com.l7tech.admin;

/**
 * The exception is used for all admins handling runtime exceptions.
 *
 * @author: ghuang
 */
public class RuntimeAdminException extends RuntimeException {

    public RuntimeAdminException() {
    }


    public RuntimeAdminException(String string) {
        super(string);
    }


    public RuntimeAdminException(String string, Throwable throwable) {
        super(string, throwable);
    }


    public RuntimeAdminException(Throwable throwable) {
        super(throwable);
    }
}
