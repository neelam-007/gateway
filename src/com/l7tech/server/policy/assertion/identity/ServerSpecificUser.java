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

        requiredLogin = specificUser.getUserLogin();
        requiredUid = specificUser.getUserUid();
        requiredProvider = specificUser.getIdentityProviderOid();
    }

    /**
     * Verifies that the authenticated <code>User</code> matches the <code>User</code>
     * corresponding to this Assertion's <code>userLogin</code> property.
     * @param requestingUser the <code>User</code> to check
     * @return <code>AssertionStatus.NONE</code> if the <code>User</code> matches.
     */
    public AssertionStatus checkUser(User requestingUser) {
        if (specificUser == null || requiredLogin == null || requiredUid == null ) {
            String msg = "null assertion or SpecificUser has null login and uid.";
            logger.warning(msg);
            return AssertionStatus.SERVER_ERROR;
        }

        // check provider id and user login (start with provider as it's cheaper)
        long requestProvider = requestingUser.getProviderId();
        String requestUid = requestingUser.getUniqueIdentifier();
        String requestLogin = requestingUser.getLogin();

        if (requestProvider == requiredProvider) {
            // Check uid first if present
            if ( requiredUid == null || requiredUid.equals(requestUid) ) {
                // They can't both be null (already checked) so this is safe
                if ( requiredLogin == null || requiredLogin.equals(requestLogin) ) {
                    return AssertionStatus.NONE;
                }
            }
        }
        logger.fine("No credentials found");
        return AssertionStatus.AUTH_FAILED;
    }

    protected SpecificUser specificUser;
    private final String requiredLogin;
    private final String requiredUid;
    private final long requiredProvider;

    protected final Logger logger = Logger.getLogger(getClass().getName());
}
