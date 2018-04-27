package com.l7tech.common.io;

public class UnsupportedX509GeneralNameException extends Exception {
    public UnsupportedX509GeneralNameException() {
        super();
    }

    public UnsupportedX509GeneralNameException(String message) {
        super(message);
    }

    public UnsupportedX509GeneralNameException(String message, Throwable th) {
        super(message,th);
    }
}
