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
        specificUser = data;
    }

    /**
     * Verifies that the authenticated <code>User</code> matches the <code>User</code>
     * corresponding to this Assertion's <code>userLogin</code> property.
     * @param requestingUser the <code>User</code> to check
     * @return <code>AssertionStatus.NONE</code> if the <code>User</code> matches.
     */
    public AssertionStatus checkUser(User requestingUser) {
        String specifiedLogin = specificUser.getUserLogin();
        String specifiedUid = specificUser.getUserUid();
        if (specificUser == null || specifiedLogin == null || specifiedUid == null ) {
            String msg = "null assertion or SpecificUser has null login and uid.";
            logger.warning(msg);
            return AssertionStatus.SERVER_ERROR;
        }

        // check provider id and user login (start with provider as it's cheaper)
        if (requestingUser.getProviderId() == specificUser.getIdentityProviderOid()) {
            String requestingUserUid = requestingUser.getUniqueIdentifier();
            // Check uid first if present
            if ( specifiedUid == null || specifiedUid.equals(requestingUserUid) ) {
                // They can't both be null (already checked) so this is safe
                if ( specifiedLogin == null || specifiedLogin.equals(requestingUser.getLogin()) ) {
                    return AssertionStatus.NONE;
                }
            }
        }
        logger.fine("No credentials found");
        return AssertionStatus.AUTH_FAILED;
    }

    protected SpecificUser specificUser;
    protected final Logger logger = Logger.getLogger(getClass().getName());
}
