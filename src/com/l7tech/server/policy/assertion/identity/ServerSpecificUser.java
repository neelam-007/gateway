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
import com.l7tech.server.message.PolicyEnforcementContext;

import java.util.logging.Logger;

import org.springframework.context.ApplicationContext;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerSpecificUser extends ServerIdentityAssertion implements ServerAssertion {
    public ServerSpecificUser(SpecificUser data, ApplicationContext applicationContext) {
        super(data, applicationContext);
        specificUser = data;

        requiredLogin = specificUser.getUserLogin();
        requiredUid = specificUser.getUserUid();
        requiredProvider = specificUser.getIdentityProviderOid();
    }

    /**
     * Verifies that the authenticated <code>User</code> matches the <code>User</code>
     * corresponding to this Assertion's <code>userLogin</code> property.
     *
     * @param requestingUser the <code>User</code> to check
     * @param context
     * @return <code>AssertionStatus.NONE</code> if the <code>User</code> matches.
     */
    public AssertionStatus checkUser(User requestingUser, PolicyEnforcementContext context) {

        // The login and the uid can't both be null
        if (requiredLogin == null && requiredUid == null) {
            String msg = "This assertion is not configured properly. The login and uid can't both be null.";
            logger.warning(msg);
            return AssertionStatus.SERVER_ERROR;
        }

        long requestProvider = requestingUser.getProviderId();
        String requestUid = requestingUser.getUniqueIdentifier();
        String requestLogin = requestingUser.getLogin();

        // provider must always match
        if (requestProvider != requiredProvider) {
            logger.fine("Authentication failed because providers id did not " +
              "match (" + requestProvider + " instead of " + requiredProvider + ").");
            return AssertionStatus.AUTH_FAILED;
        }

        // uid only needs to match if it's set as part of the assertion
        if (requiredUid != null) {
            if (!requiredUid.equals(requestUid)) {
                logger.fine("Authentication failed because the uid does not match.");
                return AssertionStatus.AUTH_FAILED;
            }
        }

        // login only needs to match if it's set as part of the assertion
        if (requiredLogin != null) {
            if (!requiredLogin.equals(requestLogin)) {
                logger.fine("Authentication failed because the login does not match.");
                return AssertionStatus.AUTH_FAILED;
            }
        }

        logger.fine("Match successful");
        return AssertionStatus.NONE;
    }

    protected SpecificUser specificUser;
    private final String requiredLogin;
    private final String requiredUid;
    private final long requiredProvider;

    protected final Logger logger = Logger.getLogger(getClass().getName());
}
