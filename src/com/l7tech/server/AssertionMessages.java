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
    public static final M SSL_CONTEXT_INIT_FAILED              = m(4000, Level.SEVERE, "Couldn't initialize SSL Context");
    public static final M HTTP_ROUTING_ASSERTION               = m(4001, Level.INFO, "Processing HTTP routing assertion");
    public static final M NON_SOAP_NOT_SUPPORTED_WRONG_FORMAT  = m(4002, Level.WARNING, "This option is not supported for non-soap messages. This message is supposed to be soap but does not appear to be.");
    public static final M NON_SOAP_NOT_SUPPORTED_WRONG_POLICY  = m(4003, Level.WARNING, "This option is not supported for non-soap messages. Something is wrong with this policy.");
    public static final M PROMOMTING_ACTOR                     = m(4004, Level.FINE, "promoting actor {0}");
    public static final M NO_SECURITY_HEADER                   = m(4005, Level.INFO, "Routing assertion asked for security header with actor {0} be promoted but there was no such security header present in the message.");
    public static final M ERROR_READING_RESPONSE               = m(4006, Level.SEVERE, "Error reading response");
    public static final M CANNOT_RESOLVE_IP_ADDRESS            = m(4007, Level.WARNING, "Couldn't resolve client IP address");
    public static final M TAI_REQUEST_NOT_AUTHENTICATED        = m(4008, Level.FINE, "TAI credential chaining requested, but request was not authenticated.");
    public static final M TAI_REQUEST_CHAIN_USERNAME           = m(4009, Level.FINE, "TAI credential chaining requested; will chain username {0}");
    public static final M TAI_REQUEST_USER_ID_NOT_UNIQUE       = m(4010, Level.WARNING, "TAI credential chaining requested, but request User did not have a unique identifier: id is {0}");
    public static final M TAI_REQUEST_CHAIN_LOGIN              = m(4011, Level.FINE, "TAI credential chaining requested, but there is no User; will chain pc.login {0}");
    public static final M TAI_REQUEST_NO_USER_OR_LOGIN         = m(4012, Level.WARNING, "TAI credential chaining requested, and request was authenticated, but had no User or pc.login");
    public static final M ADD_OUTGOING_COOKIE                  = m(4013, Level.FINE, "Adding outgoing cookie: name = {0}");
    public static final M LOGIN_INFO                           = m(4014, Level.FINE, "Using login '{0}'");
    public static final M ROUTED_OK                            = m(4015, Level.FINE, "Request routed successfully");
    public static final M RESPONSE_STATUS                      = m(4016, Level.FINE, "Protected service ({0}) responded with status {1}");
    public static final M ADD_OUTGOING_COOKIE_WITH_VERSION     = m(4017, Level.FINE, "Adding outgoing cookie: name = {0}, version = {1}");
    public static final M UPDATE_COOKIE                        = m(4018, Level.FINE,  "Updating cookie: name = {0}");


    // ServerCredentialSourceAssertion messages
    public static final M AUTH_REQUIRED             = m(4050, Level.INFO, "Authentication Required");

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
