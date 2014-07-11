package com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions;

/**
 * User: rseminoff
 * Date: 28/02/13
 */
public class MissingJwtClaimsException extends Exception {
    public MissingJwtClaimsException() {
        super();
    }
    public MissingJwtClaimsException(String s) {
        super(s);
    }
}
