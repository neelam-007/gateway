/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.identity.User;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.security.xml.processor.BadSecurityContextException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.security.GeneralSecurityException;

/**
 * The token service interface.
 * @author emil
 * @version Jan 3, 2005
 * @see TokenServiceImpl
 */
public interface TokenService {

    /**
     * Handles token service requests using a PolicyEnforcementContext. The TokenService implementation is responsible
     * for enforcing a policy that makes sense.
     *
     * @param context must contain the request at entry and will be populated with a response document if everything
     * goes well.
     * @return AssertionStatus.NONE if all is good, other return values indicate an error in which case
     * context.getFaultDetail() is to contain an error to return to the requestor
     */
    AssertionStatus respondToSecurityTokenRequest(PolicyEnforcementContext context,
                                                  CredentialsAuthenticator authenticator)
                                                  throws InvalidDocumentFormatException,
                                                         TokenServiceImpl.TokenServiceException,
                                                         ProcessorException,
                                                         BadSecurityContextException,
                                                         GeneralSecurityException,
                                                         AuthenticationException;

    /**
     * <code>CredentialsAuthenticator</code> are passed to the
     * TokenService#respondToRequestSecurityToken()
     * as authentication strategies.
     */
    public interface CredentialsAuthenticator {
        User authenticate(LoginCredentials creds) throws AuthenticationException;
    }
}