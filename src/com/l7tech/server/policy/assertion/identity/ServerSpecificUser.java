/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.server.policy.assertion.ServerAssertion;

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
     * Verifies that the authenticated <code>User</code> matches the <code>User</code>
     * corresponding to this Assertion's <code>userLogin</code> property.
     * @param requestingUser the <code>User</code> to check
     * @return <code>AssertionStatus.NONE</code> if the <code>User</code> matches.
     */
    public AssertionStatus checkUser(User requestingUser) {
        // avoid npe, we must have a login to compare to
        if (_data == null || _data.getUserLogin() == null) {
            String msg = "null assertion or SpecificUser has null login.";
            logger.warning(msg);
            return AssertionStatus.SERVER_ERROR;
        }

        // check provider id and user login (start with provider as it's cheaper
        if (requestingUser.getProviderId() == _data.getIdentityProviderOid()) {
            if (_data.getUserLogin().equals(requestingUser.getLogin())) {
                return AssertionStatus.NONE;
            }
        }
        logger.fine("No credentials found");
        return AssertionStatus.AUTH_FAILED;
    }

    protected SpecificUser _data;
    protected final Logger logger = Logger.getLogger(getClass().getName());
}
