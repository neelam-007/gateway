package com.l7tech.server;

import com.l7tech.common.Messages;

import java.util.logging.Level;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class AssertionMessages extends Messages {

    // ServerHttpRoutingAssertion messages
    public static final M SSL_CONTEXT_INIT_FAILED   = m(4000, Level.SEVERE, "Couldn't initialize SSL Context");
    public static final M HTTP_ROUTING_ASSERTION    = m(4001, Level.INFO, "Processing HTTP routing assertion");

    // ServerCredentialSourceAssertion messages
    public static final M AUTH_REQUIRED             = m(4050, Level.INFO, "Authentication Required");
    public static final M EXCEPTION                 = m(4051, Level.SEVERE, "Exception caught: ");

    // ServerIdentityAssertion
    public static final M AUTHENTICATED_BUT_CREDENTIALS_NOT_FOUND = m(4100, Level.WARNING, "Request is authenticated but request has no LoginCredentials!");
    public static final M CREDENTIALS_NOT_FOUND                   = m(4101, Level.INFO, "No credentials found!");
    public static final M ALREADY_AUTHENTICATED                   = m(4102, Level.FINEST, "Request already authenticated");
    public static final M ID_PROVIDER_ID_NOT_SET                  = m(4103, Level.SEVERE, "Can't call checkRequest() when no valid identityProviderOid has been set!");
    public static final M ID_PROVIDER_NOT_FOUND                   = m(4104, Level.SEVERE, "Couldn't find identity provider!");
    public static final M ID_PROVIDER_NOT_EXIST                   = m(4105, Level.WARNING, "id assertion refers to an id provider which does not exist anymore");
    public static final M AUTHENTICATED                           = m(4106, Level.FINEST, "Authenticated {0}");
    public static final M INVALID_CERT                            = m(4107, Level.INFO, "Invalid client cert for {0}");
    public static final M AUTHENTICATION_FAILED                   = m(4108, Level.SEVERE, "Authentication failed for {0}");

}
