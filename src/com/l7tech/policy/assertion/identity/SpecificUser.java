/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;

import java.util.logging.Level;

/**
 * Asserts that the requester is a particular User.
 *
 * @author alex
 * @version $Revision$
 */
public class SpecificUser extends IdentityAssertion {
    public SpecificUser() {
        super();
    }

    public SpecificUser( long providerId, String userLogin ) {
        super( providerId );
        _userLogin = userLogin ;
    }

    public void setUserLogin( String userLogin ) {
        if ( userLogin != _userLogin ) _user = null;
        _userLogin = userLogin;
    }

    public String getUserLogin() {
        return _userLogin;
    }

    /**
     * Attempts to resolve a <code>User</code> based on this Assertion's <code>userLogin</code> property.
     * @return a <code>User</code> that matches the login, or null if none could be found.
     * @throws FindException if
     */
    protected User getUser() throws FindException {
        if ( _user == null ) {
            UserManager uman = getIdentityProvider().getUserManager();
            _user = uman.findByLogin( _userLogin );
        }
        return _user;
    }

    /**
     * Verifies that the authenticated <code>User</code> matches the <code>User</code> corresponding to this Assertion's <code>userLogin</code> property.
     * @param u the <code>User</code> to check
     * @return <code>AssertionStatus.NONE</code> if the <code>User</code> matches.
     */
    public AssertionStatus doCheckUser( User u ) {
        try {
            if ( u.equals( getUser() ) )
                return AssertionStatus.NONE;
            else
                return AssertionStatus.AUTH_FAILED;
        } catch ( FindException fe ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, fe);
            return AssertionStatus.FAILED;
        }
    }

    protected String _userLogin;
    protected User _user;
}
