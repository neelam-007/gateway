/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.policy.assertion.AssertionStatus;

import java.security.Principal;

/**
 * Asserts that the requester is a particular User.
 *
 * @author alex
 * @version $Revision$
 */
public class SpecificUser extends IdentityAssertion {
    public SpecificUser( IdentityProvider provider, Principal user ) {
        super( provider );
        _user = user;
    }

    public SpecificUser() {
        super();
    }

    public void setUser( Principal user ) {
        _user = user;
    }

    public Principal getUser() {
        return _user;
    }

    public AssertionStatus doCheckPrincipal( Principal p ) {
        if ( p.equals( _user ) )
            return AssertionStatus.NONE;
        else
            return AssertionStatus.AUTH_FAILED;
    }

    protected Principal _user;
}
