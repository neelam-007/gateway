/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.identity.User;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.security.xml.processor.BadSecurityContextException;
import org.w3c.dom.Document;

import java.security.GeneralSecurityException;

/**
 * The token service interface.
 * @author emil
 * @version Jan 3, 2005
 * @see TokenServiceImpl
 */
public interface TokenService {
    /**
     * Handles the request for a security token (either secure conversation context or saml thing).
     * @param request the request for the secure conversation context
     * @param authenticator resolved credentials such as an X509Certificate to an actual user to associate the context with
     * @return
     */
    Document respondToRequestSecurityToken(Document request, CredentialsAuthenticator authenticator, String clientAddress)
      throws InvalidDocumentFormatException, TokenServiceImpl.TokenServiceException,
      ProcessorException, GeneralSecurityException,
      AuthenticationException, BadSecurityContextException;

    /**
     * <code>CredentialsAuthenticator</code> are passed to the
     * {@link TokenService#respondToRequestSecurityToken(org.w3c.dom.Document, com.l7tech.server.TokenService.CredentialsAuthenticator, String)
     * as authentication strategies.
     */
    public interface CredentialsAuthenticator {
        User authenticate(LoginCredentials creds) throws AuthenticationException;
    }
}