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
    public static final M AUTH_REQUIRED                        = m(4050, Level.INFO, "Authentication Required");

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

    // ServerRequestWssOperation messages
    public static final M NOTHING_TO_VALIDATE                     = m(4150, Level.FINE, "This is intended for another recipient, there is nothing to validate here.");
    public static final M CANNOT_VERIFY_WS_SECURITY               = m(4151, Level.INFO, "Request not SOAP; cannot verify WS-Security contents");
    public static final M NO_WSS_LEVEL_SECURITY                   = m(4152, Level.INFO, "This request did not contain any WSS level security.");

    // ServerRequestSwAAssertion messages
    public static final M REQUEST_NOT_SOAP                        = m(4200, Level.INFO, "Request not SOAP; cannot validate attachments");
    public static final M NOT_MULTIPART_MESSAGE                   = m(4201, Level.INFO, "The request does not contain attachment or is not a mulitipart message");
    public static final M OPERATION_NOT_FOUND                     = m(4202, Level.FINEST, "Operation not found in the request. Xpath expression is: {0}");
    public static final M SAME_OPERATION_APPEARS_MORE_THAN_ONCE   = m(4203, Level.INFO, "Same operation appears more than once in the request. Xpath expression is: {0}");
    public static final M OPERATION_IS_NON_ELEMENT_NODE           = m(4204, Level.INFO, "XPath pattern {0} found non-element node '{1}'");
    public static final M PARAMETER_IS_NON_ELEMENT_NODE           = m(4205, Level.INFO, "XPath pattern {0}/{1} found non-element node '{2}'");
    public static final M OPERATION_FOUND                         = m(4206, Level.FINEST, "The operation {0} is found in the request");
    public static final M MIME_PART_NOT_FOUND                     = m(4207, Level.FINE, "MIME Part not found in the request. Xpath expression is: {0}/{1})");
    public static final M SAME_MIME_PART_APPEARS_MORE_THAN_ONCE   = m(4208, Level.FINE, "Same MIME Part appears more than once in the request. Xpath expression is: {0}/{1}");
    public static final M PARAMETER_FOUND                         = m(4209, Level.FINEST, "Parameter {0} is found in the request");
    public static final M REFERENCE_NOT_FOUND                     = m(4210, Level.INFO, "The reference (href) of the {0} is found in the request");
    public static final M REFERENCE_FOUND                         = m(4211, Level.FINEST, "The href of the parameter {0} is found in the request, value={1}");
    public static final M INVALID_CONTENT_ID_URL                  = m(4212, Level.INFO, "Invalid Content-ID URL {0}");
    public static final M MUST_BE_ONE_OF_CONTENT_TYPES            = m(4213, Level.INFO, "The content type of the attachment {0} must be one of the types: {1}");
    public static final M INCORRECT_CONTENT_TYPE                  = m(4214, Level.INFO, "The content type of the attachment {0} must be: {1}");
    public static final M TOTAL_LENGTH_LIMIT_EXCEEDED             = m(4215, Level.INFO, "The parameter [{0}] has {1} attachments. The total length exceeds the limit: {2} K bytes");
    public static final M INDIVIDUAL_LENGTH_LIMIT_EXCEEDED        = m(4216, Level.INFO, "The length of the attachment {0} exceeds the limit: {1} K bytes");
    public static final M ATTACHMENT_NOT_FOUND                    = m(4217, Level.INFO, "The required attachment {0} is not found in the request");
    public static final M UNEXPECTED_ATTACHMENT_FOUND             = m(4218, Level.INFO, "Unexpected attachment {0} found in the request.");
    public static final M INVALID_OPERATION                       = m(4219, Level.INFO, "The operation specified in the request is invalid.");


}
