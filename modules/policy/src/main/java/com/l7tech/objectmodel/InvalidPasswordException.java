package com.l7tech.objectmodel;

/**
 * Signifies that a password does not respect the password rules.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Oct 19, 2004<br/>
 */
public class InvalidPasswordException extends ObjectModelException {
    public InvalidPasswordException() {
        super();
    }
    public InvalidPasswordException(String msg) {
        super(msg);
    }
}
