/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.AssertionError;

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

    public AssertionError doCheckPrincipal( Principal p ) {
        if ( p.equals( _user ) )
            return AssertionError.NONE;
        else
            return AssertionError.AUTH_FAILED;
    }

    protected Principal _user;
}
