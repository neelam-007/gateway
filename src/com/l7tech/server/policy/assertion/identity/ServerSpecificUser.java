/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.logging.LogManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerSpecificUser extends ServerIdentityAssertion implements ServerAssertion {
    public ServerSpecificUser( SpecificUser data ) {
        super( data );
        _data = data;
    }

    /**
     * Attempts to resolve a <code>User</code> based on this Assertion's <code>userLogin</code>
     * property.
     * @return a <code>User</code> that matches the login, or null if none could be found.
     * @throws com.l7tech.objectmodel.FindException if
     */
    protected User getUser() throws FindException {
        UserManager uman = getIdentityProvider().getUserManager();
        return uman.findByLogin( _data.getUserLogin() );
    }

    /**
     * Verifies that the authenticated <code>User</code> matches the <code>User</code>
     * corresponding to this Assertion's <code>userLogin</code> property.
     * @param requestingUser the <code>User</code> to check
     * @return <code>AssertionStatus.NONE</code> if the <code>User</code> matches.
     */
    public AssertionStatus checkUser(User requestingUser) {
        try {
            User specifiedUser = getUser();
            if (specifiedUser == null || !requestingUser.equals(specifiedUser)) {
                logger.log( Level.FINE, "Requesting user " + requestingUser.getLogin() +
                                         " does not match specified user " + _data.getUserLogin());
                return AssertionStatus.UNAUTHORIZED;
            }
            else
                return AssertionStatus.NONE;
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Exception getting user from provider", e);
            return AssertionStatus.FAILED;
        }
    }

    protected SpecificUser _data;
    protected Logger logger = LogManager.getInstance().getSystemLogger();
}
