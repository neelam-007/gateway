package com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions;

/**
 * User: rseminoff
 * Date: 28/02/13
 */
public class IllegalJwtSignatureException extends Exception {
    public IllegalJwtSignatureException(){
        super();
    }

    public IllegalJwtSignatureException(String s) {
        super(s);
    }
}
