package com.l7tech.external.assertions.xacmlpdp;

/**
 * This exception will be thrown when any required fields (ID, Data Type, and Issue Instant) are evaluated as Not Found.
 *
 * @author: ghuang
 */
public class RequiredFieldNotFoundException extends Exception {
    public RequiredFieldNotFoundException(String message) {
        super(message);
    }
}
