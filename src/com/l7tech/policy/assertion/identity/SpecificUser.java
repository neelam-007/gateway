/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

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
        _userLogin = userLogin;
    }

    public String getUserLogin() {
        return _userLogin;
    }

    protected String _userLogin;
}
