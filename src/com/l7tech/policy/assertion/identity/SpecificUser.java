/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.objectmodel.FindException;

import java.security.Principal;

import org.apache.log4j.Category;

/**
 * Asserts that the requester is a particular User.
 *
 * @author alex
 * @version $Revision$
 */
public class SpecificUser extends IdentityAssertion {
    public SpecificUser( IdentityProvider provider, Principal user ) {
        super( provider.getConfig().getOid() );
        _user = user;
    }

    public SpecificUser() {
        super();
    }

    public void setUser( Principal principal ) {
        User tempUser;
        if ( principal instanceof User ) {
            tempUser = (User)principal;
        } else {
            String err = "Principal " + principal + " is not a User!";
            _log.error( err );
            throw new RuntimeException( err );
        }

        try {
            _user = _identityProvider.getUserManager().findByLogin( tempUser.getLogin() );
        } catch ( FindException fe ) {
            _log.error( "Couldn't find user " + tempUser.getName(), fe );

        }
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

    protected transient Category _log = Category.getInstance( getClass() );
}
