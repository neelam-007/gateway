package com.l7tech.ntlm.protocol;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class AuthenticationManagerException extends Exception {
    public enum Status {
        STATUS_ERROR/* 0 */,
        STATUS_ACCOUNT_NOT_FOUND/* 1 */,
        STATUS_INVALID_CREDENTIALS/* 2 */,
        STATUS_RECOVERABLE_FAILURE/* 3 */,
        STATUS_DOMAIN_NOT_FOUND/* 4 */,
        STATUS_ENTRY_NOT_FOUND/* 5 */,
        STATUS_ALREADY_EXISTS/* 6 */,
        STATUS_ACCESS_DENIED/* 7 */
    }

    private Status code;

    public AuthenticationManagerException(String message) {
        super(message);
        this.code = Status.STATUS_ERROR;
    }

    public AuthenticationManagerException(Status code, String message) {
        super(message);
        this.code = code;
    }

    public AuthenticationManagerException(Status code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public AuthenticationManagerException(String message, Throwable cause) {
        super(message, cause);
        this.code = Status.STATUS_ERROR;
    }

    public Status getCode() {
        return this.code;
    }
}
