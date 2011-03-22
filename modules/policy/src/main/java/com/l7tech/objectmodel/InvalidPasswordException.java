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
    private String passwordPolicyDescription = null;
    public InvalidPasswordException() {
        super();
    }
    public InvalidPasswordException(String msg) {
        super(msg);
    }
    public InvalidPasswordException(String msg, String passwordPolicyDescription) {
        super(msg);
        this.passwordPolicyDescription = passwordPolicyDescription;
    }
    public String getPasswordPolicyDescription(){
        return passwordPolicyDescription;
    }
}
