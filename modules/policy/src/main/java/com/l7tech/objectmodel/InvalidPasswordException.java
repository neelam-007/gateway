package com.l7tech.objectmodel;

import java.util.Collection;
import java.util.Collections;

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
    private final String passwordPolicyDescription;
    private final Collection<String> passwordErrors;

    public InvalidPasswordException(final String msg) {
        super(msg);
        passwordPolicyDescription = null;
        passwordErrors = Collections.emptyList();
    }

    public InvalidPasswordException(final String msg,
                                    final String passwordPolicyDescription) {
        this( msg, passwordPolicyDescription, Collections.<String>emptyList() );
    }

    public InvalidPasswordException(final String msg,
                                    final String passwordPolicyDescription,
                                    final Collection<String> errors ) {
        super(msg);
        this.passwordPolicyDescription = passwordPolicyDescription;
        this.passwordErrors = Collections.unmodifiableCollection( errors );
    }

    /**
     * Get a description of the password policy.
     *
     * @return A policy description or null if not available.
     */
    public String getPasswordPolicyDescription(){
        return passwordPolicyDescription;
    }

    /**
     * Get the password error details.
     *
     * @return The error details (may be empty if not available, never null)
     */
    public Collection<String> getPasswordErrors() {
        return passwordErrors;
    }
}
